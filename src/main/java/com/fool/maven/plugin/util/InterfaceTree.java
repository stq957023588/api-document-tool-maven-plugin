package com.fool.maven.plugin.util;

import com.fool.maven.plugin.InterfaceInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class InterfaceTree {

    InterfaceTreeNode root;

    public InterfaceTree() {
        this.root = new InterfaceTreeNode();
    }

    static class InterfaceTreeNode {

        private int floor;

        private String tag;

        List<InterfaceTreeNode> nodes;

        List<InterfaceInformation> data;

        public InterfaceTreeNode() {
            nodes = new ArrayList<>();
            data = new ArrayList<>();
        }

        public InterfaceTreeNode(String tag, InterfaceTreeNode node, InterfaceInformation data) {
            this.tag = tag;
            this.nodes = new ArrayList<>();
            if (node != null) {
                this.nodes.add(node);
            }
            this.data = new ArrayList<>();
            if (data != null) {
                this.data.add(data);
            }
        }
    }


    public void print(Consumer<InterfaceInformation> interfaceInformationConsumer, Consumer<String> titleConsumer) {
        print(root, interfaceInformationConsumer, titleConsumer);
    }

    private void print(InterfaceTreeNode node, Consumer<InterfaceInformation> interfaceInformationConsumer, Consumer<String> titleConsumer) {
        if (node.tag != null) {
            StringBuilder stringBuilder = new StringBuilder();
            Stream.generate(() -> "#").limit(node.floor).forEach(stringBuilder::append);
            stringBuilder.append(" ");
            stringBuilder.append(node.tag);
            titleConsumer.accept(stringBuilder.toString());
        }

        node.data.forEach(interfaceInformationConsumer);
        if (node.nodes.isEmpty()) {
            return;
        }
        node.nodes.forEach(e -> this.print(e, interfaceInformationConsumer, titleConsumer));
    }

    public void put(InterfaceInformation interfaceInformation) {
        put(interfaceInformation, root);
    }

    private void put(InterfaceInformation interfaceInformation, InterfaceTreeNode node) {
        if (interfaceInformation.getTags() == null || interfaceInformation.getTags().isEmpty()) {
            node.data.add(interfaceInformation);
            return;
        }

        String tag = interfaceInformation.getTags().remove(0);

        for (InterfaceTreeNode n : node.nodes) {
            if (n.tag.equals(tag)) {
                put(interfaceInformation, n);
                return;
            }
        }

        InterfaceTreeNode child = new InterfaceTreeNode();
        node.nodes.add(child);
        child.tag = tag;
        child.floor = node.floor + 1;
        put(interfaceInformation, child);

    }


}
