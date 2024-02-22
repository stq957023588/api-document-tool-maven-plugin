package com.fool.maven.plugin;

import java.util.List;

public class InterfaceInformation {

    private String[] path;

    private String[] method;

    private List<ApiParameter> apiParameters;

    private Class<?> returnType;

    private String[] returnTypeGenericType;

    private String description;

    private List<String> tags;

    public InterfaceInformation() {
    }


    public InterfaceInformation(String[] path, String[] method, List<ApiParameter> apiParameters, Class<?> returnType, String[] returnTypeGenericType, String description, List<String> tags) {
        this.path = path;
        this.method = method;
        this.apiParameters = apiParameters;
        this.returnType = returnType;
        this.returnTypeGenericType = returnTypeGenericType;
        this.description = description;
        this.tags = tags;
    }

    public String[] getReturnTypeGenericType() {
        return returnTypeGenericType;
    }

    public void setReturnTypeGenericType(String[] returnTypeGenericType) {
        this.returnTypeGenericType = returnTypeGenericType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public String[] getPath() {
        return path;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public String[] getMethod() {
        return method;
    }

    public void setMethod(String[] method) {
        this.method = method;
    }

    public List<ApiParameter> getApiParameters() {
        return apiParameters;
    }

    public void setApiParameters(List<ApiParameter> apiParameters) {
        this.apiParameters = apiParameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
