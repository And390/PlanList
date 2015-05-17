package template;

import org.apache.commons.lang.StringEscapeUtils;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Потоко-безопасный (за потоко-безопасность методов getTemplate и putTemplate отвечают наследники)
 * Для одиночного контента (без экземпляра TemplateManager, include будет недоступен)
 * пользовательский код может использовать: TemplateManager.parse(content).eval(bindings, output)
 * В общем случае: templateManager.eval(path, bindings, output);
 * или равносильно: templateManager.getTemplate(path).eval(bindings, output)
 * Пути: относительный в include - значит относительно каталога текущего шаблона, абсолютный - относительно корня шаблонов;
 *  getTemplate (и сам TemplateManager) должен принимать только абсолютные пути в этом смысле.
 * Все пути хранятся с Unix-слэшами (даже если работает это под Windows)
 * And390 - 03.12.13
 */
public abstract class TemplateManager
{
    //    синглетон для ScriptEngine
    private static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private static ScriptEngine scriptEngine;

    public synchronized static ScriptEngine getEngine()
    {
        if (scriptEngine!=null)  return scriptEngine;
        return scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
    }

    // По-хорошему, надо проверять scriptEngine.getFactory().getParameter("THREADING")!=null,
    // нас устраивает минимальный уровень MULTITHREADED, иначе для null документация пишет:
    // "The engine implementation is not thread safe, and cannot be used to execute scripts concurrently on multiple threads".
    // Rhino возвращает MULTITHREADED, Nashorn - null, но его классы ScriptEngine и CompiledScript потокобезопасны
    // (видимо, Nashorn могу бы возващать значение MULTITHREADED,
    //  http://stackoverflow.com/questions/30140103/should-i-use-a-separate-scriptengine-and-compiledscript-instances-per-each-threa).
    // То есть в итоге TemplateManager работает из предположения, что сам объект ScriptEngine потокобезопасен.

    public static void free()
    {
        BindedTemplate.currContext.remove();
    }

    // возвращает (создает на свое усмотрение) шаблон по указанному пути или null, если не найдено
    public abstract Template getTemplate(String path) throws Exception;

    // сохраняет шаблон по указанному пути, если template==null, то удаляет
    public abstract void putTemplate(String path, Template template);

    protected static String checkPath(String path)
    {
        if (!path.startsWith("/"))  throw new RuntimeException ("Template path must be absolute (starts with '/'): "+path);
        if (path.indexOf('\\')!=-1)  throw new RuntimeException ("Template path contains forbidden character '\\'");
        return path;
    }

    public void eval(String path, Bindings bindings, Appendable out) throws Exception
    {
        Template template = getTemplate(path);
        if (template==null)  throw new IOException ("Resource is not found: "+path);
        template.eval(bindings, out);
    }

    public boolean evalIfExists(String path, Bindings bindings, Appendable out) throws Exception
    {
        Template template = getTemplate(path);
        if (template==null)  return false;
        template.eval(bindings, out);
        return true;
    }

    // A little more usefull than new SimpleBindings(). For Nashorn it provide a values reading after eval()
    public static Bindings createBindings()  {  return getEngine().createBindings();  }


    //                --------    parsing    --------

    // parse - главная функция, которая превращает контент в шаблон
    // если указан не null manager, то смогут работать функции include из шаблона (которые иначе вызывают ошибку),
    // но, если используются относительные пути, то должен быть указан и path (иначе include вызовет ошибку)

    public static Template parse(String content) throws ScriptException  {  return parse(content, null, null);  }

    public static Template parse(String content, TemplateManager manager, String path) throws ScriptException
    {
        int[] pos = new int [] { 0 };
        Template template = parse(content, pos, manager, path);
        if (pos[0]!=-1)  throw new ScriptException ("Unexpected end of template (unexpected <$: )");
        return template;
    }

    public static Template parseAndPut(String content, TemplateManager manager, String path) throws ScriptException
    {
        Template template = parse(content, manager, path);
        manager.putTemplate(path, template);
        return template;
    }

    private static final String initScript =
            "function include(path) { context.include(path); }; " +
            "function output(text) { context.output(text); }; " +
            "function evaluate(text) { context.evaluate(text); }; ";

    private static Template parse(String content, int[] pos, TemplateManager manager, String path) throws ScriptException
    {
        StringBuilder buffer = new StringBuilder(initScript);
        ArrayList<Template> childs = null;
        ArrayList<String> strings = new ArrayList<> ();

        int start=pos[0];
        int i=start;
        int i0;
        for (;;)
        {
            //    найти следующий ${ или <$
            boolean tagged = false;
            i0 = i;
            for (;;)  {
                i = content.indexOf('$', i);
                if (i==-1)  break;  //end of content
                else if (i!=0 && content.charAt(i-1)=='<')  {  tagged=true;  i--;  break;  }
                else if (i!=0 && content.charAt(i-1)=='{')  {  i--;  break;  }  //экранирование
                else if (i+1!=content.length() && content.charAt(i+1)=='{')  break;
                else  i++;
            }
            //    добавить вставку текста до ${ или <$
            if (i!=i0 && (i0!=0 || i!=-1))  {
                buffer.append(" context.outputResponsePart(").append(strings.size()).append(");");
                strings.add(content.substring(i0, i==-1 ? content.length() : i));
            }
            //    достигнут ли конец
            if (i==-1)  {  pos[0]=-1;  break;  }
            //    может быть это экранирование $
            if (content.startsWith("<$>", i) || content.startsWith("{$}", i))  {
                buffer.append(" context.output('$');");
                i += 3;
                continue;
            }
            else if (content.startsWith("${$}", i))  {
                buffer.append(" context.output('$$');");
                i += "${$}".length();
                continue;
            }
            //    терминатор <$:
            if (tagged && i+2!=content.length() && content.charAt(i+2)==':')  {  pos[0]=i+3;  break;  }  //i до скобок (понадобится потом), но курсор передвинуть за <$:
            //    переходим к разбору скрипта внутри скобок
            i += 2;  //${ or <$ length
            //    найти закрывающую } или $>
            while (true)  {  //цикл из-за возможных дочерних шаблонов
                i0 = i;
                int expressionsOpen = -1;
                boolean singleQuote = false;
                boolean doubleQuote = false;
                boolean blockCommentOpen = false;
                boolean lineCommentOpen = false;
                boolean slash = false;
                for (int bracketCounter=1; ; i++)  {
                    if (i==content.length())  throw new ScriptException (tagged ? "missing $>" : "missing }");
                    char c = content.charAt(i);
                    if (singleQuote)  {
                        if (c=='\'')  if (!slash)  singleQuote = false;
                        slash = c=='\\';
                    }
                    else if (doubleQuote)  {
                        if (c=='\"')  if (!slash)  doubleQuote = false;
                        slash = c=='\\';
                    }
                    else if (blockCommentOpen)  {
                        if (c=='*' && i+1!=content.length() && content.charAt(i+1)=='/')  {  blockCommentOpen = false;  i++;  }
                    }
                    else if (lineCommentOpen)  {
                        if (c=='\n')  {  lineCommentOpen = false;  }
                    }
                    else
                        if (c=='{')  bracketCounter++;
                        else if (c=='}')  {
                            bracketCounter--;
                            if (bracketCounter==0 && !tagged)  break;
                            expressionsOpen = -1;
                        }
                        else if (c=='$' && i+1!=content.length() && content.charAt(i+1)=='>')
                            if (tagged)  break;  else throw new ScriptException ("encountered whrong chars $> inside ${}");
                        else if (c=='<' && i+1!=content.length() && content.charAt(i+1)=='$')
                            throw new ScriptException (tagged ? "encountered open chars <$ again, may be missing $>" : "encountered whrong chars <$ inside ${, may be missing }");
                        else if (c=='$' && i+1!=content.length() && content.charAt(i+1)=='{')
                            throw new ScriptException (tagged ? "encountered whrong chars ${ inside <$, may be missing $>" : "encountered open chars ${ again, may be missing }");
                        else if (c=='/' && i+1!=content.length() && content.charAt(i+1)=='*')  {  blockCommentOpen = true;  i++;  }
                        else if (c=='/' && i+1!=content.length() && content.charAt(i+1)=='/')  {  lineCommentOpen = true;  i++;  }
                        else if (c==';')  expressionsOpen = -1;
                        else if (c>' ')  {
                            if (expressionsOpen==-1)  expressionsOpen = i;
                            if (c=='\'')  singleQuote = true;
                            else if (c=='\"')  doubleQuote = true;
                        }
                }
                //    дочерние шаблоны
                if (tagged && content.charAt(i-1)==':' && i!=i0)  {
                    //    добавить скрипт
                    buffer.append(content.substring(i0, i - 1));
                    //    рекурсивно разобрать дочерний шаблон
                    i += 2;
                    pos[0] = i;
                    if (childs==null)  childs = new ArrayList<> ();
                    childs.add(parse(content, pos, manager, path));
                    i = pos[0];
                    //    добавить скрипт, возвращающий шаблон
                    buffer.append(" context.childs[").append(childs.size()-1).append("]");
                    //    должен остановиться на открытых скобках, которые будут разбраны следующей итерацией цикла
                    if (i==-1)  throw new ScriptException ("Child template is not ends with <$:");
                    continue;
                }
                //    добавить скрипт мeжду скобками
                if (i!=i0)
                    //    закрытое выражение, не возвращает значения - просто добавить скрипт
                    if (expressionsOpen==-1 || tagged)  buffer.append(content.substring(i0, i));
                    //    открытое выражение, которое возвращает значение - добавить скрипт, затем вывод значения
                    else  {
                        buffer.append(content.substring(i0, expressionsOpen));
                        if (endsWith(content, "#h", i))  buffer.append(" context.output(context.escapeHTML(")
                                .append(content.substring(expressionsOpen, i - "#h".length())).append("));");
                        else if (endsWith(content, "#j", i))  buffer.append(" context.output(context.escapeJavaScript(")
                                .append(content.substring(expressionsOpen, i - "#j".length())).append("));");
                        else  buffer.append(" context.output(").append(content.substring(expressionsOpen, i)).append(");");
                    }
                break;
            }
            i += tagged ? "$>".length() : "}".length();
        }

        //    сформировать результат
        //    если динамических элементов нет, вернуть статичный шаблон
        if (i0==start)  {
            return new StaticTemplate (content.substring(i0, i==-1 ? content.length() : i));
        }
        else  {
            String script = buffer.toString();
            ScriptEngine engine = getEngine();
            //    если можно скомпилировать, вернуть скомпилированный шаблон
            if (engine instanceof Compilable)
                return new CompiledTemplate (script, strings, childs, manager, path);
            //    иначе обычный интерпретируемый
            else
                return new ScriptTemplate (script, strings, childs, manager, path);
        }
    }

    private static boolean endsWith(String string, String prefix, int offset)  {
        return string.startsWith(prefix, offset - prefix.length());
    }


    //                --------    template implamantations    --------

    // Статичный шаблон, просто возвращающий фиксированный текст
    public static class StaticTemplate implements Template
    {
        public String content;
        public StaticTemplate(String content)  {  this.content = content;  }

        public void eval(Bindings bindings, Appendable out) throws IOException
        {
            out.append(content);
        }
    }

    // Динамический шаблон
    public static abstract class BindedTemplate implements Template
    {
        private static final ThreadLocal<Context> currContext = new ThreadLocal<> ();

        public final TemplateManager manager;
        public final String path;
        private final String[] parts;  //must be read-only for thread safe
        private final Template[] childs;  //must be read-only for thread safe

        public BindedTemplate(TemplateManager manager, String path, Collection<String> parts, Collection<Template> childs)  {
            this.manager = manager;
            this.path = path==null ? null : checkPath(path);
            this.parts = parts==null ? null : parts.toArray(new String [parts.size()]);
            this.childs = childs==null ? null : childs.toArray(new Template [childs.size()]);
        }

        public void eval(Bindings bindings, Appendable out) throws ScriptException, NoSuchMethodException
        {
            Context context = new Context(manager, path, parts, childs, bindings, out);
            //    сохранить состояние контекста
            Context lastContext = currContext.get();
            currContext.set(context);
            //    eval
            eval(context, out);
            //    если вызов eval был внутри другого eval (include), то теперь текущий контекст сохранится в движке
            //    вместо родительского, поэтому необходимо восстановить состояние контекста (Scripting API не делает этого сам)
            currContext.set(lastContext);
            if (lastContext!=null) {
                context.set(lastContext.manager, lastContext.path, lastContext.parts, lastContext.childs,
                        lastContext.bindings, lastContext.out);
            }
        }

        public abstract void eval(ScriptContext context, Appendable out) throws ScriptException;

        @SuppressWarnings("unused")
        public static final class Context implements ScriptContext
        {
            public TemplateManager manager;
            public String path;
            public String[] parts;  //must be read-only for thread safe, may be it better be private
            public Template[] childs;  //must be read-only for thread safe, may be it better be private
            public Bindings bindings;
            public Appendable out;

            public Context(TemplateManager manager, String path, String[] parts, Template[] childs, Bindings bindings, Appendable out)  {
                set(manager, path, parts, childs, bindings, out);
            }

            public void set(TemplateManager manager, String templatePath, String[] parts, Template[] childs, Bindings bindings, Appendable out)  {
                this.manager = manager;
                this.path = templatePath;
                this.parts = parts;
                this.childs = childs;
                this.bindings = bindings;
                this.out = out;
            }

            public void output(String string) throws IOException
            {
                out.append(string!=null ? string : "");
            }

            public void outputResponsePart(int index) throws IOException
            {
                out.append(parts[index]);
            }

            public void include(String path) throws Exception
            {
                if (manager==null)  throw new ScriptException ("Can not include template without TemplateManager");
                String sourcePath = path;
                //    обработать относительный путь
                if (!path.startsWith("/"))
                {
                    if (this.path ==null)  throw new ScriptException ("Can not resolve relative template path because no base path is specified");
                    //    игнорировать обращение к текущему каталогу
                    if (path.startsWith("./"))  path = path.substring("./".length());
                    //    посчитать корневой каталог
                    int p = this.path.lastIndexOf('/');
                    if (p==-1)  throw new RuntimeException ("Path must starts with '/': "+ this.path);
                    while (path.startsWith("../"))  {
                        path = path.substring("../".length());
                        p = this.path.lastIndexOf('/', p - 1);
                        if (p==-1)  throw new ScriptException ("Wrong parent path: "+sourcePath+", relative to "+ this.path);
                    }
                    //    сложить корневой каталог и относительный путь
                    path = this.path.substring(0, p + 1) + path;
                }
                //    внутри пути не должно быть обращений к родительскому или текущему каталгу, виндовый слэш запрещаем
                if (path.contains("/./") || path.endsWith("/.") || path.equals("."))  throw new ScriptException ("Parent path references ('..') are allowed only at the start of a path: "+sourcePath);
                if (path.contains("/../") || path.endsWith("/..") || path.equals(".."))  throw new ScriptException ("Current path references ('.') are allowed only at the start of a path: "+sourcePath);
                if (path.indexOf('\\')!=-1)  throw new ScriptException ("Path contains forbidden character '\\'");
                //    выполнить
                manager.eval(path, bindings, out);
            }

            public void evaluate(Template template) throws Exception
            {
                template.eval(new ChildBindings (bindings), out);
            }

            public String escapeHTML(String string)
            {
                return StringEscapeUtils.escapeHtml(string);
            }

            public String escapeJavaScript(String string)
            {
                return StringEscapeUtils.escapeJavaScript(string);
            }

            public String escapeXML(String string)
            {
                return StringEscapeUtils.escapeXml(string);
            }

            //        ----    ScriptContext realisation    ----

            public void setBindings(Bindings bindings, int scope)  {
                if (scope!=ENGINE_SCOPE)  throw new IllegalArgumentException("Illegal scope value.");
                this.bindings = bindings;
            }

            public Bindings getBindings(int scope)  {
                if (scope!=ENGINE_SCOPE)  throw new IllegalArgumentException("Illegal scope value.");
                return bindings;
            }

            public void setAttribute(String name, Object value, int scope)  {
                if (scope!=ENGINE_SCOPE)  throw new IllegalArgumentException("Illegal scope value.");
                bindings.put(name, value);
            }

            public Object getAttribute(String name, int scope)  {
                if (scope!=ENGINE_SCOPE)  throw new IllegalArgumentException("Illegal scope value.");
                return bindings.get(name);
            }

            public Object removeAttribute(String name, int scope)  {
                if (scope!=ENGINE_SCOPE)  throw new IllegalArgumentException("Illegal scope value.");
                return bindings.remove(name);
            }

            public Object getAttribute(String name)  {  return bindings.get(name);  }
            public int getAttributesScope(String name)  {  return bindings.containsKey(name) ? ENGINE_SCOPE : -1;  }
            public Writer getWriter()  {  throw new UnsupportedOperationException ();  }
            public Writer getErrorWriter()  {  throw new UnsupportedOperationException ();  }
            public void setWriter(Writer writer)  {  throw new UnsupportedOperationException ();  }
            public void setErrorWriter(Writer writer)  {  throw new UnsupportedOperationException ();  }
            public Reader getReader()  {  throw new UnsupportedOperationException ();  }
            public void setReader(Reader reader)  {  throw new UnsupportedOperationException ();  }
            public List<Integer> getScopes()  {  throw new UnsupportedOperationException ();  }
        }
    }

    // Интерпретируемый шаблон, хранит текст скрипта и выпоняет его при каждом вызове eval
    public static class ScriptTemplate extends BindedTemplate
    {
        public final String script;

        public ScriptTemplate(String script, Collection<String> strings, Collection<Template> childs, TemplateManager manager, String templatePath)  {
            super(manager, templatePath, strings, childs);
            this.script = script;
        }

        public void eval(ScriptContext context, Appendable out) throws ScriptException
        {
            getEngine().eval(script, context);
        }
    }

    // Скомпилированный шаблон, отличается от ScriptTemplate тем, что хранит скомпилированную версию скрипта
    // Эта версия для потокобезопасной реализации ScriptEngine.
    public static class CompiledTemplate extends BindedTemplate
    {
        public final CompiledScript script;

        public CompiledTemplate(String script, Collection<String> strings, Collection<Template> childs,
                                TemplateManager manager, String templatePath) throws ScriptException
        {
            super(manager, templatePath, strings, childs);
            this.script = compile(scriptEngine, script, templatePath);
        }

        public void eval(ScriptContext context, Appendable out) throws ScriptException
        {
            script.eval(context);
        }
    }

    private static CompiledScript compile(ScriptEngine engine, String content, String path) throws ScriptException
    {
        engine.getContext().setAttribute(ScriptEngine.FILENAME, path, ScriptContext.ENGINE_SCOPE);
        CompiledScript result = ((Compilable)engine).compile(content);
        engine.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
        return result;
    }
}
