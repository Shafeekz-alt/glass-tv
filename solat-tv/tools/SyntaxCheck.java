import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;

/** Syntax check only, intentionally does not substitute for compiling against Android SDK. */
public class SyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
             var paths = Files.walk(Path.of("app/src/main/java"))) {
            List<Path> sources = paths.filter(p -> p.toString().endsWith(".java")).toList();
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null, files.getJavaFileObjectsFromPaths(sources));
            task.parse();
            boolean failed = false;
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) if (d.getKind() == Diagnostic.Kind.ERROR) { System.err.println(d); failed = true; }
            if (failed) System.exit(1);
            System.out.println("PASS: Java syntax parsed for " + sources.size() + " source files (Android compilation not performed).");
        }
    }
}
