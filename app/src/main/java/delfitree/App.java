/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package delfitree;

import java.io.File;

import delfitree.exceptions.NotADirException;
import delfitree.exceptions.UnhandledIncludeType;

public class App
{
    private IncludeType getIncludeType(String includeType)
    {
        switch (includeType) {
            case "none":
                return IncludeType.None;
            case "iface":
                return IncludeType.Interface;
            case "impl":
                return IncludeType.Implementation;
            case "both":
                return IncludeType.Both;
            default:
                throw new UnhandledIncludeType(includeType);
        }
    }

    public void run(String[] args)
    {
        if (args.length < 3) {
            System.out.println("java -jar app.jar <sources> <out> uses root [prefix] [unit]");
            System.out.println("  <sources> путь к файлам Delfi");
            System.out.println("  <out>     путь к папке вывода");
            System.out.println("  uses      none|iface|impl|both");
            System.out.println("  root      root|all");
            System.out.println("  prefix    опциональный префикс модулей");
            System.out.println("  unit      опциональное имя модуля");
        } else {
            String filesDirParam = args[0];
            String outDirParam = args[1];
            String includeTypeParam = args[2];
            String rootParam = args.length > 3 ? args[3] : "";
            String prefixParam = args.length > 4 ? args[4] : "";
            String unitParam = args.length > 5 ? args[5] : "";

            File filesDir = new File(filesDirParam);
            File outDir = new File(outDirParam);
            IncludeType includeType = getIncludeType(includeTypeParam);
            boolean justRoot = rootParam.equals("root") ? true : false;

            if (!filesDir.isDirectory()) {
                throw new NotADirException(filesDirParam);
            } else if (!outDir.isDirectory()) {
                throw new NotADirException(outDirParam);
            } else {
                System.out.println(String.format("Путь к папке: %s", filesDir));
                System.out.println(String.format("Папка вывода: %s", outDir));
                System.out.println(String.format("Режим       : %s", includeType));
                System.out.println(String.format("Корни       : %b", includeType));
                System.out.println(String.format("Префик      : %s", prefixParam));
                System.out.println(String.format("Модуль      : %s", unitParam));

                Forest nodes = new TreeParser(filesDir).makeTree(includeType, prefixParam, unitParam);
                new GraphBuilder(nodes).build(justRoot).flush(outDir);
            }
        }
    }

    public static void main(String[] args) {
        new App().run(args);
    }
}