package template;

import javax.script.Bindings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * And390 - 23.04.2015
 */
public class ChildBindings extends HashMap<String, Object> implements Bindings
{
    private Bindings parent;

    public ChildBindings(Bindings parent)
    {
        this.parent = parent;
    }

    @Override
    public Object put(String name, Object value) {
        // Nashorn do not call this method anyway for update variables, so let's child variables can not touch parent variables
        //if (super.containsKey(name))  return super.put(name, value);
        //else if (parent.containsKey(name))  return parent.put(name, value);
        //else  return super.put(name, value);
        return super.put(name, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        super.putAll(toMerge);
    }

    @Override
    public boolean containsKey(Object key) {
        boolean result = super.containsKey(key);
        return result || parent.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        Object result = super.get(key);
        return result!=null ? result : parent.get(key);
    }

    @Override
    public Object remove(Object key) {
        return super.remove(key);
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public boolean containsValue(Object value) {
        boolean result = super.containsValue(value);
        return result || parent.containsValue(value);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException ();
    }
}
