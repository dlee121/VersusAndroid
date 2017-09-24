package com.vs.bcd.versus.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dlee on 6/12/17.
 */

public class VSCNode {
    private VSCNode head_sibling = null;    //sibling which is displayed on top of the this node's comment
    private VSCNode tail_sibling = null;    //sibling which is displayed under this node's comment
    private VSCNode first_child = null;
    private VSCNode parent = null;
    private VSComment nodeContent;

    public VSCNode(VSComment vsc){
        nodeContent = vsc;
    }

    public VSCNode(VSComment vsc, VSCNode parent){
        nodeContent = vsc;
        this.parent = parent;
    }

    public void setParent(VSCNode parent){
        this.parent = parent;
    }

    public VSCNode getFirstChild(){
        return first_child;
    }

    public void setFirstChild(VSCNode child){
        first_child = child;
    }

    public VSComment getNodeContent(){
        return this.nodeContent;
    }

    public void setNodeContent(VSComment nodeContent){
        this.nodeContent = nodeContent;
    }

    public boolean isRoot(){
        //constructor ensures nodeContent is not null
        return (nodeContent.getParent_id().trim().equals("0"));
    }

    public void removeParent(){
        this.parent = null;
    }

    public void setHeadSibling(VSCNode node){
        head_sibling = node;
    }

    public void setTailSibling(VSCNode node){
        tail_sibling = node;
    }

    public VSCNode getHeadSibling(){
        return head_sibling;
    }

    public VSCNode getTailSibling(){
        return tail_sibling;
    }

    public VSCNode getParent(){
        return parent;
    }

    public String getCommentID(){
        if(nodeContent != null)
            return nodeContent.getComment_id();
        else
            return null;
    }

    public String getParentID(){
        if(nodeContent != null)
            return nodeContent.getParent_id();
        else
            return null;
    }


    public boolean hasHeadSibling(){
        return head_sibling != null;
    }

    public boolean hasTailSibling(){
        return tail_sibling != null;
    }
    public boolean hasChild(){
        return first_child != null;
    }
    public boolean hasParent(){
        return parent != null;
    }

    //also returns the VSComment object, since this is used during List.add operation
    public VSComment setNestedLevelandGetComment(int level){
        nodeContent.setNestedLevel(level);
        return nodeContent;
    }

    public void setNestedLevel(int level){
        nodeContent.setNestedLevel(level);
    }

    public int getUpvotes(){
        return nodeContent.getUpvotes();
    }

    public int getDownvotes(){
        return nodeContent.getDownvotes();
    }

    public int getVotecount(){
        return nodeContent.votesCount();
    }

    public void updateUservoteAndDecrement(int uservote){
        nodeContent.updateUservoteAndDecrement(uservote);
    }

    public void updateUservote(int uservote){
        nodeContent.updateUservote(uservote);
    }

    public void setUserVote(int uservote){
        nodeContent.setUservote(uservote);
    }

    public void setTopMedal(int topMedal){
        nodeContent.setTopmedal(topMedal);
    }

    public int getCurrentMedal(){
        return nodeContent.getCurrentMedal();
    }
    public void setCurrentMedal(int currentMedal){
        nodeContent.setCurrentMedal(currentMedal);
    }

    public String getTimestamp(){
        return nodeContent.getTimestamp();
    }

}
