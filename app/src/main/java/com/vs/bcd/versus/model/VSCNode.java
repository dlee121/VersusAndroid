package com.vs.bcd.versus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dlee on 6/12/17.
 */

public class VSCNode {
    private VSCNode forward_sibling = null;
    private VSCNode backward_sibling = null;
    private VSCNode first_child = null;
    private VSCNode parent = null;
    private VSComment nodeContent;
    private List<VSCNode> children = new ArrayList<>();

    public VSCNode(VSComment vsc){
        nodeContent = vsc;
    }

    public VSCNode(VSComment vsc, VSCNode parent){
        nodeContent = vsc;
        this.parent = parent;
    }

    public List<VSCNode> getChildren(){
        return children;
    }

    public void setParent(VSCNode parent){
        parent.addChild(this);
        this.parent = parent;
    }

    public void addChild(VSCNode child){
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(VSComment vsc){
        VSCNode child = new VSCNode(vsc);
        child.setParent(this);
        this.children.add(child);
    }

    public VSComment getNodeContent(){
        return this.nodeContent;
    }

    public void setNodeContent(VSComment nodeContent){
        this.nodeContent = nodeContent;
    }

    public boolean isRoot(){
        return (this.parent == null);
    }

    //leaf has no children
    public boolean isLeaf(){
        return (this.children.size() == 0);
    }

    public void removeParent(){
        this.parent = null;
    }
}
