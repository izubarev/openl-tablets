package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.richfaces.model.TreeNode;

public class CollectionParameterTreeNode extends ParameterDeclarationTreeNode {
    private static final String COLLECTION_TYPE = "collection";
    protected final ParameterRenderConfig config;

    public CollectionParameterTreeNode(ParameterRenderConfig config) {
        super(config.getFieldNameInParent(), config.getValue(), config.getType(), config.getParent());
        this.config = config;
    }

    @Override
    public String getDisplayedValue() {
        return Utils.displayNameForCollection(getType(), isLeaf());
    }

    @Override
    public String getNodeType() {
        return COLLECTION_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        if (isValueNull()) {
            return new LinkedHashMap<>();
        } else {
            Iterator<Object> iterator = getType().getAggregateInfo().getIterator(getValue());
            IOpenClass collectionElementType = getType().getComponentClass();
            int index = 0;
            LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = new LinkedHashMap<>();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                IOpenClass type = collectionElementType;
                if (element != null && element.getClass() != collectionElementType.getInstanceClass()) {
                    // Show content of complex objects
                    type = JavaOpenClass.getOpenClass(element.getClass());
                }

                ParameterRenderConfig childConfig = new ParameterRenderConfig.Builder(type, element)
                    .keyField(
                        config.getKeyField() != null ? config.getKeyField() : collectionElementType.getIndexField())
                    .parent(this)
                    .hasExplainLinks(config.isHasExplainLinks())
                    .requestId(config.getRequestId())
                    .build();

                elements.put(index, ParameterTreeBuilder.createNode(childConfig));
                index++;
            }
            return elements;
        }
    }

    @Override
    protected Object constructValueInternal() {
        IAggregateInfo info = getType().getAggregateInfo();
        IOpenClass componentType = info.getComponentType(getType());
        int elementsCount = getChildrenMap().size();
        Object ary = info.makeIndexedAggregate(componentType, elementsCount);

        IOpenIndex index = info.getIndex(getType());

        for (int i = 0; i < elementsCount; i++) {
            ParameterDeclarationTreeNode node = getChildrenMap().get(i);
            Object value;
            if (node instanceof CollectionParameterTreeNode) {
                value = node.getValueForced();
            } else {
                node.getValueForced();
                value = getNodeValue(node);
            }
            Object key = getKeyFromElementNum(i);
            if (value == null) {
                value = getNullableValue();
            }
            if (key != null) {
                index.setValue(ary, key, value);
            }
        }
        return ary;
    }

    @Override
    public void addChild(Object elementNum, TreeNode element) {
        int nextChildNum = getChildren().size();
        Object value = element == null ? getNullableValue() : ((ParameterDeclarationTreeNode) element).getValue();
        ParameterDeclarationTreeNode node = createNode(null, value);
        if (nextChildNum > 0) {
            initComplexNode(getChild(nextChildNum - 1), node);
        }
        super.addChild(nextChildNum, node);
        saveChildNodesToValue();
    }

    /**
     * @return {@code null} value or default value for primitives
     */
    private Object getNullableValue() {
        IOpenClass elementType = getType().getComponentClass();
        return elementType.nullObject();
    }

    protected void initComplexNode(ParameterDeclarationTreeNode from, ParameterDeclarationTreeNode to) {
        if (!(to instanceof ComplexParameterTreeNode)) {
            return;
        }
        ComplexParameterTreeNode complexNode = (ComplexParameterTreeNode) to;
        IOpenClass type = from.getType();
        if (from instanceof ComplexParameterTreeNode) {
            IOpenClass typeToCreate = ((ComplexParameterTreeNode) from).getTypeToCreate();
            if (typeToCreate != null) {
                type = typeToCreate;
            }
        }
        complexNode.setTypeToCreate(type);
    }

    public void removeChild(ParameterDeclarationTreeNode toDelete) {
        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            if (node == toDelete) {
                super.removeChild(i);
                break;
            }
            i++;
        }

        // Create new value based on changed child elements count
        saveChildNodesToValue();
        // Children keys in children map must be remapped because element in the middle was deleted
        updateChildrenKeys();
    }

    protected void updateChildrenKeys() {
        LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = getChildrenMap();
        // Values in LinkedHashMap are in the same order as they were inserted before
        List<ParameterDeclarationTreeNode> values = new ArrayList<>(elements.values());
        // Reinsert values with new keys
        elements.clear();
        for (int index = 0; index < values.size(); index++) {
            elements.put(index, values.get(index));
        }
    }

    @Override
    public void replaceChild(ParameterDeclarationTreeNode oldNode, ParameterDeclarationTreeNode newNode) {
        super.replaceChild(oldNode, newNode);
        saveChildNodesToValue();
    }

    protected ParameterDeclarationTreeNode createNode(Object key, Object value) {
        ParameterRenderConfig childConfig = new ParameterRenderConfig.Builder(getType().getComponentClass(), value)
            .keyField(config.getKeyField())
            .parent(this)
            .hasExplainLinks(config.isHasExplainLinks())
            .requestId(config.getRequestId())
            .build();

        return ParameterTreeBuilder.createNode(childConfig);
    }

    @SuppressWarnings("unchecked")
    private void saveChildNodesToValue() {
        IOpenClass arrayType = getType();
        IAggregateInfo info = arrayType.getAggregateInfo();
        Object newCollection = info.makeIndexedAggregate(arrayType.getComponentClass(), getChildren().size());
        IOpenIndex index = info.getIndex(arrayType);
        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            Object key = getKeyFromElementNum(i);
            Object value = getNodeValue(node);
            if (value == null) {
                value = getNullableValue();
            }
            if (key != null) {
                if(index!=null){
                    index.setValue(newCollection, key, value);
                }else if(newCollection instanceof Collection){
                    ((Collection<Object>) newCollection).add(value);
                }
            }
            i++;
        }
        setValue(newCollection);
    }

    protected Object getKeyFromElementNum(int elementNum) {
        if (elementNum >= getChildren().size()) {
            return getChildren().size();
        }
        return elementNum;
    }

    protected Object getNodeValue(ParameterDeclarationTreeNode node) {
        return node.getValue();
    }
}
