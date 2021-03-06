package delfitree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import delfitree.exceptions.TreeParserException;

public class TreeParser
{
    private final File filesDir;

    public TreeParser(File filesDir)
    {
        this.filesDir = filesDir;
    }

    private String[] listModules()
    {
        return Arrays.asList(filesDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File filesDir, String name)
            {
                String patterns[] = {
                    ".*\\.pas"
                };

                for (String strPattern : patterns) {
                    Pattern pattern = Pattern.compile(strPattern);
                    Matcher matcher = pattern.matcher(name.toLowerCase());

                    if (matcher.find()) {
                        return true;
                    }
                }

                return false;
            }
        }))
        .stream()
        .map(f -> f.getName()
        .replace(".pas", ""))
        .toArray(String[]::new);
    }

    private String readFile(File inputFile) throws FileNotFoundException, IOException
    {
        StringBuilder resultStringBuilder = new StringBuilder();

        try (
            FileInputStream inputStream = new FileInputStream(inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }

        return resultStringBuilder.toString();
    }

    private List<Node> getDependencies(
        String regExp,
        String content,
        Forest forest,
        IncludeType includeType,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(regExp, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        List<Node> dependecies = new LinkedList<>();

        if (matcher.find())
        {
            String includes = matcher.group(1);
            includes = includes.replaceAll("\\{\\$IFDEF [^\\}]+\\}", "");
            includes = includes.replaceAll("\\{\\$IFNDEF [^\\}]+\\}", "");
            includes = includes.replaceAll("\\{\\$ELSE\\}", "");
            includes = includes.replaceAll("\\{\\$ENDIF\\}", "");

            final String regExpInner = String.format("(%s[^\\s,]+)", prefix);
            Pattern patternInner = Pattern.compile(regExpInner, Pattern.MULTILINE);
            Matcher matcherInner = patternInner.matcher(includes);

            while (matcherInner.find())
            {
                String subModuleName = matcherInner.group(0);
                Node subModuleNode = parseModule(subModuleName, forest, includeType, prefix);

                dependecies.add(subModuleNode);
            }
        }

        return dependecies;
    }

    private Node parseModule(
        String moduleName,
        Forest forest,
        IncludeType includeType,
        String prefix
    ) {
        Node moduleNode = forest.getNodeByName(moduleName);

        if (moduleNode == null) {
            moduleNode = new Node(moduleName);
            forest.add(moduleNode);

            String content;
            try {
                String folderName = filesDir.toString();
                String fileName = String.format("%s.pas", moduleName);
                File module = new File(folderName, fileName);
                if (module.exists())
                {
                    content = readFile(module);
                } else {
                    moduleNode.setCustom(false);
                    return moduleNode;
                }
            } catch (IOException e) {
                throw new TreeParserException(e.getMessage());
            }

            String regExp;
            List<Node> dependencies;

            if (includeType.enabledFor(IncludeType.Interface)) {
                regExp = "^interface\\s+uses[\\s]+([^;]+);$";
                dependencies = getDependencies(
                    regExp,
                    content,
                    forest,
                    includeType,
                    prefix
                );

                for (Node node : dependencies) {
                    node.setRoot(false);
                    moduleNode.addIface(node);
                }
            }

            if (includeType.enabledFor(IncludeType.Implementation)) {
                regExp = "^implementation\\s+uses[\\s]+([^;]+);$";
                dependencies = getDependencies(
                    regExp,
                    content,
                    forest,
                    includeType,
                    prefix
                );

                for (Node node : dependencies) {
                    node.setRoot(false);
                    moduleNode.addImpl(node);
                }
            }
        }

        return moduleNode;
    }

    public Forest makeTree(
        IncludeType includeType,
        String prefix,
        String unit
    ) {
        String[] modules;

        if (unit == null || unit.isEmpty()) {
            modules = listModules();
        } else {
            modules = new String[] { unit };
        }

        Forest forest = new Forest();

        for (String moduleName : modules)
        {
            parseModule(moduleName, forest, includeType, prefix);
        }

        forest.clear();
        return forest;
    }
}
