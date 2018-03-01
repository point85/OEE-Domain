package org.point85.domain.script;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;

public class ResolverFunction {
	private String scriptFunction;
	private String name;
	private List<String> arguments;
	private String body;

	public ResolverFunction(String script) throws Exception {
		if (script == null) {
			return;
		}
		// prep for execution
		//engine.eval(script);
		setScriptFunction(script);
	}

	private void parseFunction() throws Exception {
		if (scriptFunction == null) {
			return;
		}

		// name
		int idx1 = scriptFunction.indexOf('(');
		name = scriptFunction.substring(9, idx1);

		// args
		int idx2 = scriptFunction.indexOf(')');
		String argList = scriptFunction.substring(idx1 + 1, idx2);
		String[] args = argList.split(",");
		arguments = new ArrayList<String>(args.length);

		for (String arg : args) {
			arguments.add(arg.trim());
		}

		// body
		int idx3 = scriptFunction.indexOf('{');
		body = scriptFunction.substring(idx3 + 1, scriptFunction.lastIndexOf('}'));
	}

	public static String generateFunctionName() {
		long now = Instant.now().getEpochSecond();
		return "f" + Long.toHexString(now);
	}

	public String getName() throws Exception {
		if (name == null) {
			parseFunction();
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getArguments() throws Exception {
		if (arguments == null) {
			parseFunction();
		}
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public String getBody() throws Exception {
		if (body == null) {
			parseFunction();
		}
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getScriptFunction() {
		return scriptFunction;
	}

	public void setScriptFunction(String scriptFunction) throws Exception {
		this.scriptFunction = scriptFunction;
		parseFunction();
	}

	public Object invoke(ScriptEngine engine, Object... args) throws Exception {
		engine.eval(scriptFunction);
		return ((Invocable) engine).invokeFunction(getName(), args);
	}

	public static String functionFromBody(String script) {
		return "function " + ResolverFunction.generateFunctionName() + "(context, value, lastValue) {" + script + "}";
	}

	public String getDisplayString() {
		String displayString = "";

		if (body != null) {
			displayString = body;
			int idx = body.indexOf('\n');

			if (idx != -1) {
				displayString = body.substring(0, idx) + " ...";
			}
		}
		return displayString;
	}

}
