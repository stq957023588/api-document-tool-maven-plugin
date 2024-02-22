package com.fool.maven.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fool.maven.plugin.data.formatter.parameter.CompositeParameterGenerator;
import com.fool.maven.plugin.data.formatter.tag.CompositeTagGetter;
import com.fool.maven.plugin.util.CommonUtils;
import com.fool.maven.plugin.util.InterfaceTree;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static com.fool.maven.plugin.util.CommonUtils.*;

@Mojo(name = "api-document-generate",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.COMPILE)
public class ApiDocumentGenerateMojo extends AbstractMojo {

    @Parameter
    private String apiDocumentSavePath;

    @Parameter(defaultValue = "false")
    private boolean openDocumentAfterGenerated;

    @Parameter(defaultValue = "false")
    private boolean unwindClassField;

    /**
     * Practical reference to the Maven project
     */
    @Parameter(required = true, defaultValue = "${project}")
    private MavenProject project;

    private Class<? extends Annotation> requestMappingAnnClass;

    private CompositeTagGetter tagGetter;

    private CompositeParameterGenerator parameterGenerator;

    private ObjectMapper objectMapper;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project == null) {
            getLog().error("Can't get project!");
            return;
        }
        getLog().info(String.format("Generate %s project api document...", project.getName()));

        Context.initialize(project);
        Context.setUnwindClassField(unwindClassField);
        objectMapper = new ObjectMapper();

        tagGetter = new CompositeTagGetter();
        parameterGenerator = new CompositeParameterGenerator();

        // 获取编译输出目录
        String outputDirectoryPath = project.getBuild().getOutputDirectory();
        File outputDirectory = new File(outputDirectoryPath);
        getLog().info(String.format("OutputDirectory:%s", outputDirectoryPath));

        String sourceDirectoryPath = project.getBuild().getSourceDirectory();

        Map<String, String> classNamePathMap = getClassNames(outputDirectory, sourceDirectoryPath, outputDirectoryPath);
        Set<String> classNames = classNamePathMap.keySet();

        // 创建类加载器
        try {
            // 加载类
            List<Class<?>> controllerClasses = loadControllerClass(classNames);
            List<InterfaceInformation> interfaceInformations = loadInterface(controllerClasses, classNamePathMap);
            InterfaceTree interfaceTree = loadInterfaceTree(interfaceInformations);
            File document = generateDocument(interfaceTree);
            openDocument(document);
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading classes", e);
        }
    }

    public void openDocument(File file) {
        if (!openDocumentAfterGenerated) {
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            getLog().warn("Desktop is not supported");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (file.exists()) {
            try {
                desktop.open(file);
            } catch (IOException e) {
                getLog().warn("打开文件失败");
            }
        }

    }


    public File generateDocument(InterfaceTree interfaceTree) {

        MdKiller.SectionBuilder sectionBuilder = MdKiller.of();

        Consumer<InterfaceInformation> consumer = getInterfaceInformationConsumer(sectionBuilder);

        Consumer<String> titleConsumer = sectionBuilder::text;

        interfaceTree.print(consumer, titleConsumer);

        Set<String> totalStore = new HashSet<>(Context.getEntityClassNames());
        List<String> entityClassNames = new ArrayList<>(Context.getEntityClassNames());
        if (!entityClassNames.isEmpty()) {
            sectionBuilder.text("# 实体类");
        }
        StringBuilder parameterTypeBuilder = new StringBuilder();

        for (int i = 0; i < entityClassNames.size(); i++) {
            String entityClassName = entityClassNames.get(i);

            try {
                Class<?> aClass = Context.getMavenProjectClassLoader().loadClass(entityClassName);
                if (aClass.getClassLoader() == null) {
                    continue;
                }
                List<ApiParameter> apiParameters = parameterGenerator.generateParameterFromClass(aClass, null);
                sectionBuilder.text(String.format("## <span name=\"%s\">%s</span>", aClass.getName(), aClass.getSimpleName()));
                Object[] tableTitle = {"参数名称", "参数类型", "释义", "备注"};
                Object[][] tableData = new Object[apiParameters.size()][4];
                for (int j = 0; j < apiParameters.size(); j++) {
                    getFieldName(apiParameters.get(j), parameterTypeBuilder);
                    tableData[j] = new Object[]{apiParameters.get(j).getName(), parameterTypeBuilder.toString(), apiParameters.get(j).getDescription(), ""};
                    parameterTypeBuilder.delete(0, parameterTypeBuilder.length());
                }
                sectionBuilder.table()
                        .data(tableTitle, tableData)
                        .endTable();
            } catch (ClassNotFoundException e) {
                getLog().warn(String.format("Class not found:%s", entityClassName));
            }

            Set<String> temp = new HashSet<>(Context.getEntityClassNames());
            temp.removeAll(totalStore);
            entityClassNames.addAll(temp);
            totalStore.addAll(temp);
        }


        String build = sectionBuilder.build();
        File file = new File(apiDocumentSavePath + File.separator + project.getName() + ".md");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            boolean mkdirs = parentFile.mkdirs();
            if (!mkdirs) throw new RuntimeException("创建接口文档失败！");
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(build.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getLog().info(String.format("Generate document success!Output path:%s", file.getAbsolutePath()));

        return file;

    }

    public void getFieldName(ApiParameter apiParameter, StringBuilder parameterTypeBuilder) {
        if (apiParameter.getType().getClassLoader() == null) {
            parameterTypeBuilder.append(apiParameter.getType().getSimpleName());
        } else {
            parameterTypeBuilder.append("[")
                    .append(apiParameter.getType().getSimpleName())
                    .append("]")
                    .append("(#")
                    .append(apiParameter.getType().getName())
                    .append(")");
        }

        if (apiParameter.getGenericType() != null) {
            generateGenericType(parameterTypeBuilder, apiParameter.getGenericType());
        }
    }

    private void generateGenericType(StringBuilder parameterTypeBuilder, String[] genericTypes) {
        if (genericTypes == null || genericTypes.length == 0) {
            return;
        }
        String[] links = new String[genericTypes.length];
        for (int i = 0; i < links.length; i++) {
            links[i] = String.format("[%s](#%s)", genericTypes[i].substring(genericTypes[i].lastIndexOf(".") + 1), genericTypes[i]);
        }
        parameterTypeBuilder.append("\\<");
        parameterTypeBuilder.append(CommonUtils.join(links));
        parameterTypeBuilder.append(">");
    }


    private Consumer<InterfaceInformation> getInterfaceInformationConsumer(MdKiller.SectionBuilder sectionBuilder) {
        Object[] parameterTableTitle = {"参数名称", "参数类型", "是否必填", "参数位置", "释义", "备注"};
        Object[] returnTableTitle = {"参数名称", "参数类型", "释义", "备注"};
        return interfaceInformation -> {

            Object[][] parameterTableDataArr = new Object[interfaceInformation.getApiParameters().size()][6];
            int tableDataIndex = 0;
            StringBuilder parameterTypeBuilder = new StringBuilder();
            for (ApiParameter apiParameter : interfaceInformation.getApiParameters()) {
                getFieldName(apiParameter, parameterTypeBuilder);
                Object[] tableData = {apiParameter.getName(), parameterTypeBuilder.toString(), "", apiParameter.getPosition(), apiParameter.getDescription(), ""};
                parameterTableDataArr[tableDataIndex++] = tableData;
                parameterTypeBuilder.delete(0, parameterTypeBuilder.length());
            }


            if (interfaceInformation.getDescription() != null
                    && !interfaceInformation.getDescription().isBlank()) {
                sectionBuilder.text(interfaceInformation.getDescription());
            }

            sectionBuilder.text(String.format("接口地址：%s", join(interfaceInformation.getPath())))
                    .text(String.format("请求方法：%s", join(interfaceInformation.getMethod())));

            if (parameterTableDataArr.length != 0) {
                sectionBuilder.text("请求参数：")
                        .table()
                        .data(parameterTableTitle, parameterTableDataArr)
                        .endTable();
            }


            Class<?> returnType = interfaceInformation.getReturnType();

            if (CommonUtils.isSubFromJdkClass(returnType) && !returnType.getSimpleName().equals("void")) {
                StringBuilder genericTypeBuilder = new StringBuilder();
                generateGenericType(genericTypeBuilder, interfaceInformation.getReturnTypeGenericType());
                Object[][] returnTableDataArr = new Object[][]{{"", returnType.getSimpleName() + genericTypeBuilder.toString(), "", ""}};
                sectionBuilder.text("响应报文：")
                        .table()
                        .data(returnTableTitle, returnTableDataArr)
                        .endTable();

                if (Collection.class.isAssignableFrom(returnType)) {
                    String genericClassName = interfaceInformation.getReturnTypeGenericType()[0];
                    try {
                        Class<?> aClass = Context.getMavenProjectClassLoader().loadClass(genericClassName);
                        Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();
                        Object o = declaredConstructor.newInstance();
                        ArrayList<Object> objects = new ArrayList<>();
                        objects.add(o);
                        String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objects);
                        sectionBuilder.text("报文样例：").text("```json").text(s).text("```");
                    } catch (Exception e) {
                        getLog().warn(String.format("generate return value sample error! GenericClassName:%s,Exception:%s", genericClassName, e.getMessage()));
                    }
                }

            } else if (returnType.getClassLoader() != null) {
                List<ApiParameter> apiParameters = parameterGenerator.generateParameterFromClass(returnType, null);
                Object[][] returnTableDataArr = new Object[apiParameters.size()][4];
                int returnTableDataIndex = 0;
                for (ApiParameter apiParameter : apiParameters) {
                    getFieldName(apiParameter, parameterTypeBuilder);
                    Object[] tableData = {apiParameter.getName(), parameterTypeBuilder.toString(), apiParameter.getDescription(), ""};
                    returnTableDataArr[returnTableDataIndex++] = tableData;
                    parameterTypeBuilder.delete(0, parameterTypeBuilder.length());
                }
                sectionBuilder.text("响应报文：")
                        .table()
                        .data(returnTableTitle, returnTableDataArr)
                        .endTable();

                try {
                    Constructor<?> constructor = returnType.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Object o = constructor.newInstance();
                    String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
                    sectionBuilder.text("报文样例：").text("```json").text(s).text("```");

                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException | JsonProcessingException e) {
                    getLog().warn(String.format("Generate sample error!%s", returnType));
                }

            }

        };
    }

    public InterfaceTree loadInterfaceTree(List<InterfaceInformation> interfaceInformations) {
        InterfaceTree interfaceTree = new InterfaceTree();
        for (InterfaceInformation interfaceInformation : interfaceInformations) {
            interfaceTree.put(interfaceInformation);
        }
        return interfaceTree;
    }


    public List<InterfaceInformation> loadInterface(List<Class<?>> classes, Map<String, String> classNamePathMap) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        ArrayList<InterfaceInformation> interfaceInformations = new ArrayList<>();

        for (Class<?> clazz : classes) {
            // 获取class注释上定义的模块（标题），例如：一级标题/二级标题
            String javaFilePath = classNamePathMap.get(clazz.getName());
            List<String> classTags = tagGetter.classTags(clazz, javaFilePath);
            if (classTags.isEmpty()) {
                classTags.add(clazz.getSimpleName());
            }

            // 获取地址前缀
            Annotation classRequestMapping = (Annotation) Context.getFindMergedAnnotationMethod().invoke(null, clazz, requestMappingAnnClass);
            String[] prefixArr = new String[]{""};
            String[] classRequestMappingPaths = annotationMethodInvoke(classRequestMapping, "path", String[].class);
            if (classRequestMappingPaths != null && classRequestMappingPaths.length == 0) {
                classRequestMappingPaths = annotationMethodInvoke(classRequestMapping, "value", String[].class);
            }

            if (classRequestMappingPaths != null && classRequestMappingPaths.length != 0) {
                prefixArr = new String[classRequestMappingPaths.length];
                for (int i = 0; i < prefixArr.length; i++) {
                    prefixArr[i] = formatPath(classRequestMappingPaths[i]);
                }
            }


            for (Method method : clazz.getDeclaredMethods()) {
                // 获取method注释上定义的模块，优先选择方法上的模块
                List<String> interfaceTags = tagGetter.methodTags(clazz, javaFilePath, method);
                if (interfaceTags.isEmpty()) {
                    interfaceTags.add(method.getName());
                }
                String description = tagGetter.methodDescription(clazz, javaFilePath, method);
                if (!interfaceTags.isEmpty() && interfaceTags.get(interfaceTags.size() - 1).equals(description)) {
                    description = null;
                }

                interfaceTags.addAll(0, classTags);

                Annotation requestMapping = (Annotation) Context.getFindMergedAnnotationMethod().invoke(null, method, requestMappingAnnClass);
                if (requestMapping == null) {
                    continue;
                }

                List<ApiParameter> apiParameters = parameterGenerator.generate(method);


                String[] pathArr = annotationMethodInvoke(requestMapping, "path", String[].class);
                if (pathArr.length == 0) {
                    pathArr = annotationMethodInvoke(requestMapping, "value", String[].class);
                }
                if (pathArr.length == 0) {
                    pathArr = new String[]{""};
                }

                String[] totalPathArr = new String[prefixArr.length * pathArr.length];
                int totalPathArrIndex = 0;
                for (String prefix : prefixArr) {
                    for (String path : pathArr) {
                        totalPathArr[totalPathArrIndex++] = prefix + formatPath(path);
                    }
                }

                Object[] requestMappingMethods = annotationMethodInvoke(requestMapping, "method", Object[].class);

                String[] httpMethodArr = new String[requestMappingMethods.length];
                for (int i = 0; i < requestMappingMethods.length; i++) {
                    httpMethodArr[i] = requestMappingMethods[i].toString();
                }

                Type genericReturnType = method.getGenericReturnType();
                String[] returnGenericType = filedGenericTypes(genericReturnType);

                InterfaceInformation interfaceInformation = new InterfaceInformation(totalPathArr, httpMethodArr, apiParameters, method.getReturnType(), returnGenericType, description, interfaceTags);
                interfaceInformations.add(interfaceInformation);
            }
        }

        return interfaceInformations;
    }

    public List<Class<?>> loadControllerClass(Collection<String> classNames) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        Class<? extends Annotation> restControllerAnnClass;
        try {
            restControllerAnnClass = (Class<? extends Annotation>) Context.getMavenProjectClassLoader().loadClass("org.springframework.web.bind.annotation.RestController");
            requestMappingAnnClass = (Class<? extends Annotation>) Context.getMavenProjectClassLoader().loadClass("org.springframework.web.bind.annotation.RequestMapping");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (String className : classNames) {
            try {
                Class<?> aClass = Context.getMavenProjectClassLoader().loadClass(className);
                Annotation annotation = aClass.getAnnotation(restControllerAnnClass);
                if (annotation == null) {
                    continue;
                }
                classes.add(aClass);
                getLog().debug(String.format("load class \"%s\" success", className));
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                getLog().warn(String.format("Class \"%s\" not found", className));
            }
        }

        return classes;
    }

    public Map<String, String> getClassNames(File directory, String sourceDirectoryPath, String outputDirectoryPath) {
        if (directory == null) {
            return new HashMap<>();
        }
        HashMap<String, String> namePathMap = new HashMap<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return new HashMap<>();
        }
        for (File file : files) {
            if (file.isDirectory()) {
                namePathMap.putAll(getClassNames(file, sourceDirectoryPath, outputDirectoryPath));
                continue;
            }
            if (file.getAbsolutePath().endsWith(".class")) {
                String className = file.getAbsolutePath()
                        .replace(outputDirectoryPath + File.separator, "")
                        .replace(File.separator, ".")
                        .replace(".class", "");

                namePathMap.put(className, file.getAbsolutePath());
            }
        }
        return namePathMap;
    }

}
