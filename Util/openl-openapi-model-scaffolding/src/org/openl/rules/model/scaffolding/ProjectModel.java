package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectModel {

    private String name;
    private boolean isRuntimeContextProvided;
    private List<DatatypeModel> datatypeModels = new ArrayList<>();
    private List<SpreadsheetModel> spreadsheetModels = new ArrayList<>();
    private List<PathInfo> pathInfos = new ArrayList<>();
    /*
     * Spreadsheets which will be generate through interface. for case, when isRuntimeContextProvided is true, but these
     * spreadsheets don't have it.
     */
    private List<SpreadsheetModel> notOpenLModels = new ArrayList<>();

    public ProjectModel() {
    }

    public ProjectModel(String name,
            boolean isRuntimeContextProvided,
            List<DatatypeModel> datatypeModels,
            List<SpreadsheetModel> spreadsheetModels,
            List<PathInfo> pathInfos,
            List<SpreadsheetModel> modelsForInterface) {
        this.name = name;
        this.isRuntimeContextProvided = isRuntimeContextProvided;
        this.datatypeModels = datatypeModels;
        this.spreadsheetModels = spreadsheetModels;
        this.pathInfos = pathInfos;
        this.notOpenLModels = modelsForInterface;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DatatypeModel> getDatatypeModels() {
        return datatypeModels;
    }

    public List<SpreadsheetModel> getSpreadsheetResultModels() {
        return spreadsheetModels;
    }

    public boolean isRuntimeContextProvided() {
        return isRuntimeContextProvided;
    }

    public List<PathInfo> getPathInfo() {
        return pathInfos;
    }

    public List<SpreadsheetModel> getModelsToClass() {
        return notOpenLModels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectModel that = (ProjectModel) o;

        if (isRuntimeContextProvided != that.isRuntimeContextProvided) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(datatypeModels, that.datatypeModels)) {
            return false;
        }
        if (!Objects.equals(pathInfos, that.pathInfos)) {
            return false;
        }
        if (!Objects.equals(spreadsheetModels, that.spreadsheetModels)) {
            return false;
        }
        return Objects.equals(notOpenLModels, that.notOpenLModels);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isRuntimeContextProvided ? 1 : 0);
        result = 31 * result + (datatypeModels != null ? datatypeModels.hashCode() : 0);
        result = 31 * result + (spreadsheetModels != null ? spreadsheetModels.hashCode() : 0);
        result = 31 * result + (notOpenLModels != null ? notOpenLModels.hashCode() : 0);
        result = 31 * result + (pathInfos != null ? pathInfos.hashCode() : 0);
        return result;
    }
}