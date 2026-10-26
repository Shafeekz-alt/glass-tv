using System.Diagnostics;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Reflection;
using System.Text;
using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;
using System.Windows.Forms;

namespace GlassTvKtSaver;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        Application.SetHighDpiMode(HighDpiMode.PerMonitorV2);
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        var option = args.FirstOrDefault()?.ToLowerInvariant() ?? "/s";
        if (option.StartsWith("/c") || option.StartsWith("-c"))
        {
            MessageBox.Show("Glass TV Kuala Terengganu\n\nZon solat: TRG01 (e-Solat JAKIM)\nHadis: 42 ringkasan, bertukar setiap 5 minit\n\nPaparan screensaver tanpa butang atau ikon aplikasi. Gerakkan tetikus atau tekan kekunci untuk keluar.",
                "Glass TV KT - Tetapan", MessageBoxButtons.OK, MessageBoxIcon.Information);
            return;
        }
        if (option.StartsWith("/p") || option.StartsWith("-p"))
        {
            var handleText = args.Length > 1 ? args[1] : option.Length > 3 ? option[3..] : "";
            if (long.TryParse(handleText, out long hwnd) && hwnd != 0)
                Application.Run(new SaverWindow(null, new IntPtr(hwnd), null));
            return;
        }
        if (option is not ("/s" or "-s" or "/d" or "-d")) return;
        Application.Run(new SaverContext());
    }
}

internal sealed class SaverContext : ApplicationContext
{
    private readonly List<SaverWindow> windows = new();
    private bool ending;
    public SaverContext()
    {
        Cursor.Hide();
        foreach (var monitor in Screen.AllScreens)
            windows.Add(new SaverWindow(monitor, IntPtr.Zero, this));
        foreach (var window in windows) window.Show();
        if (windows.Count > 0) MainForm = windows[0];
    }
    public void CloseAll()
    {
        if (ending) return;
        ending = true;
        foreach (var window in windows.ToArray())
            if (!window.IsDisposed) window.Close();
        Cursor.Show();
        ExitThread();
    }
    protected override void ExitThreadCore()
    {
        if (!ending)
        {
            ending = true;
            foreach (var window in windows.ToArray())
                if (!window.IsDisposed) window.Close();
            Cursor.Show();
        }
        base.ExitThreadCore();
    }
}

internal sealed class SaverWindow : Form
{
    private readonly IntPtr previewParent;
    private readonly SaverContext? owner;
    private readonly WebView2 browser = new();
    private readonly System.Windows.Forms.Timer inputTimer = new() { Interval = 150 };
    private readonly Point startCursor;
    private readonly Stopwatch sinceStart = Stopwatch.StartNew();
    private readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(11) };
    private bool exiting;
    private bool requestingPrayer;
    private bool isPreview => previewParent != IntPtr.Zero;

    public SaverWindow(Screen? monitor, IntPtr parent, SaverContext? context)
    {
        previewParent = parent;
        owner = context;
        ShowInTaskbar = false;
        FormBorderStyle = FormBorderStyle.None;
        BackColor = Color.Black;
        KeyPreview = true;
        TopMost = !isPreview;
        StartPosition = FormStartPosition.Manual;
        if (monitor is not null) Bounds = monitor.Bounds;
        if (isPreview)
        {
            var style = GetWindowLong(Handle, -16);
            SetWindowLong(Handle, -16, (style | 0x40000000) & ~unchecked((int)0x80000000));
            SetParent(Handle, previewParent);
            if (GetClientRect(previewParent, out var r)) Bounds = new Rectangle(0, 0, r.Right, r.Bottom);
        }
        startCursor = Cursor.Position;
        browser.Dock = DockStyle.Fill;
        browser.DefaultBackgroundColor = Color.Black;
        Controls.Add(browser);
        Shown += async (_, _) => await StartBrowserAsync();
        inputTimer.Tick += (_, _) => CheckInput();
        inputTimer.Start();
        KeyDown += (_, _) => RequestExit();
    }

    private async Task StartBrowserAsync()
    {
        try
        {
            string folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "GlassTVKT", "WebView2");
            Directory.CreateDirectory(folder);
            var env = await CoreWebView2Environment.CreateAsync(userDataFolder: folder);
            await browser.EnsureCoreWebView2Async(env);
            browser.CoreWebView2.Settings.AreDefaultContextMenusEnabled = false;
            browser.CoreWebView2.Settings.AreDevToolsEnabled = false;
            browser.CoreWebView2.Settings.IsStatusBarEnabled = false;
            browser.CoreWebView2.Settings.IsZoomControlEnabled = false;
            browser.CoreWebView2.WebMessageReceived += BrowserMessage;
            using var input = Assembly.GetExecutingAssembly().GetManifestResourceStream("GlassTvKtSaver.index.html")
                ?? throw new IOException("Embedded UI not found");
            using var reader = new StreamReader(input, Encoding.UTF8);
            browser.NavigateToString(await reader.ReadToEndAsync());
        }
        catch (Exception ex)
        {
            Controls.Clear();
            Controls.Add(new Label {
                Dock = DockStyle.Fill, ForeColor = Color.White,
                BackColor = Color.FromArgb(24, 18, 32), TextAlign = ContentAlignment.MiddleCenter,
                Text = "Glass TV KT screensaver gagal bermula.\n" + ex.Message +
                    "\nSila semak Microsoft Edge WebView2 Runtime." });
        }
    }

    private async void BrowserMessage(object? sender, CoreWebView2WebMessageReceivedEventArgs e)
    {
        try
        {
            using var json = JsonDocument.Parse(e.WebMessageAsJson);
            var action = json.RootElement.GetProperty("action").GetString();
            if (action == "exit") { RequestExit(); return; }
            if (action == "prayers") await LoadPrayerAsync();
        }
        catch (Exception ex) { Debug.WriteLine(ex); }
    }

    private async Task LoadPrayerAsync()
    {
        if (requestingPrayer || browser.CoreWebView2 is null) return;
        requestingPrayer = true;
        try
        {
            string? payload = null;
            string source = "e-Solat JAKIM";
            string? lastError = null;
            var urls = new (string Url, string Source)[] {
                ("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=TRG01", "e-Solat JAKIM"),
                ("https://solat.my/api/daily/TRG01", "Solat.my (sandaran data JAKIM)")
            };
            foreach (var item in urls)
            {
                try
                {
                    using var response = await http.GetAsync(item.Url);
                    response.EnsureSuccessStatusCode();
                    var content = await response.Content.ReadAsStringAsync();
                    if (content.Length > 524288) throw new IOException("Respons terlalu besar");
                    using var parsed = JsonDocument.Parse(content);
                    if (parsed.RootElement.ValueKind != JsonValueKind.Object)
                        throw new FormatException("Format JSON tidak disokong");
                    payload = content;
                    source = item.Source;
                    break;
                }
                catch (Exception ex) { lastError = ex.Message; }
            }
            var envelope = payload is null
                ? JsonSerializer.Serialize(new { error = lastError ?? "Sambungan tidak tersedia" })
                : "{\"source\":" + JsonSerializer.Serialize(source) + ",\"payload\":" + payload + "}";
            await browser.CoreWebView2.ExecuteScriptAsync("window.onNativePrayer(" + JsonSerializer.Serialize(envelope) + ")");
        }
        catch (Exception ex) { Debug.WriteLine(ex); }
        finally { requestingPrayer = false; }
    }

    private void CheckInput()
    {
        if (isPreview || exiting || sinceStart.ElapsedMilliseconds < 1350) return;
        var p = Cursor.Position;
        if (Math.Abs(p.X - startCursor.X) + Math.Abs(p.Y - startCursor.Y) > 14)
        {
            RequestExit();
            return;
        }
        for (int k = 1; k < 255; k++)
        {
            if ((GetAsyncKeyState(k) & 0x8000) != 0) { RequestExit(); return; }
        }
    }

    private void RequestExit()
    {
        if (isPreview || exiting) return;
        exiting = true;
        owner?.CloseAll();
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        inputTimer.Stop();
        inputTimer.Dispose();
        http.Dispose();
        browser.Dispose();
        base.OnFormClosed(e);
    }

    [DllImport("user32.dll")] private static extern short GetAsyncKeyState(int key);
    [DllImport("user32.dll", SetLastError = true)] private static extern IntPtr SetParent(IntPtr child, IntPtr parent);
    [DllImport("user32.dll")] private static extern int GetWindowLong(IntPtr hwnd, int index);
    [DllImport("user32.dll")] private static extern int SetWindowLong(IntPtr hwnd, int index, int value);
    [DllImport("user32.dll")] [return: MarshalAs(UnmanagedType.Bool)] private static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [StructLayout(LayoutKind.Sequential)] private struct RECT { public int Left, Top, Right, Bottom; }
}
