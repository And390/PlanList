package planlist;

import utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * And390 - 19.03.15.
 */
public class PlanNode
{
    public String name;
    public String title;
    public ArrayList<PlanNode> childs;  // TODO наверное, нужен hashMap

    public PlanNode()  {}
    public PlanNode(String name_, String title_, PlanNode... childs_)  {
        name=name_;  title=title_;
        if (childs_.length!=0)  {  childs = new ArrayList<> ();  childs.addAll(Arrays.asList(childs_));  }
    }

    // возвращает дочерний узел с указанным именем, пока перебором
    public PlanNode getChild(String name)  {
        for (PlanNode child : childs)  if (child.name.equals(name))  return child;
        return null;
    }


    @Override
    public String toString()  {
        StringList result = new StringList ();
        toString(result, "");
        return result.toString();
    }
    public void toString(StringList result, String tab)  {
        result.append(name).append('\t').append(title);
        if (childs!=null)  {
            tab += " ";
            for (PlanNode child : childs)  child.toString(result.append('\n').append(tab), tab);
        }
    }

    @Override
    public boolean equals(Object o)  {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanNode planNode = (PlanNode) o;

        if (childs != null ? !childs.equals(planNode.childs) : planNode.childs != null) return false;
        if (name != null ? !name.equals(planNode.name) : planNode.name != null) return false;
        if (title != null ? !title.equals(planNode.title) : planNode.title != null) return false;

        return true;
    }

    @Override
    public int hashCode()  {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (childs != null ? childs.hashCode() : 0);
        return result;
    }
}
