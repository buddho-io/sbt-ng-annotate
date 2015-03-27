/*global process, require */

(function() {
	"use strict";
	
	var fs = require("fs"),
		jst = require("jstranspiler"),
		nodefn = require("when/node"),
		mkdirp = require("mkdirp"),
		path = require("path"),
		ngAnnotate = require("ng-annotate");

	var promised = {
		mkdirp: nodefn.lift(mkdirp),
		readFile: nodefn.lift(fs.readFile),
		writeFile: nodefn.lift(fs.writeFile) 
	};

	var args = jst.args(process.argv);

	var opts = args.options;

	if(opts.add == null) {
		opts.add = true;
	}
	if(opts.remove == null) {
		opts.remove = false;
	}

	function processor(input, output) {
		return promised.readFile(input, "utf8").then(function(contents) {

			// create a defensive copy of the options
			var o = JSON.parse(JSON.stringify(opts));

			if(o.sourcemap) {
				o.map = {inline: true, inFile: input, sourceRoot: input};
			}

			var ret =  ngAnnotate(contents, o);

			if(ret.errors) {
				throw parseError(input, contents, ret.errors);
			} else {
				return ret;
			}

		}).then(function(result) {
			return promised.mkdirp(path.dirname(output)).yield(result);
		}).then(function(result) {
			var p = (result.map) ?
				promised.writeFile(output, result.src, "utf8").then(
					promised.writeFile(output + ".map", result.map, "utf8")
				) : promised.writeFile(output, result.src, "utf8");

			return p.yield(result);
		}).then(function(result) {
			return {
				source: input,
				result: {
					filesRead: [input],
					filesWritten: [output]
				}
			}
		}).catch(function(e) {
			if (jst.isProblem(e)) return e; else throw e;
		});
	}

	jst.process({processor: processor, inExt: ".js", outExt: ".js"}, args);

	/**
	 * Utility to take an error object and coerce it into a Problem object.
	 */
	function parseError(input, contents, errors) {
		var errLines = errors;
		var lineNumber = (errLines.length > 1 ? errLines[1].substr(errLines[1].indexOf(":") + 1, 1) : 0);
		var charOffset = (errLines.length > 1 ? errLines[1].substr(errLines[1].indexOf(":") - 1, 1) : 0);
		var lines = contents.split("\n", lineNumber);
		return {
			message: errLines.length > 0 ? errLines[0] : errors, //err.name + ": " + (errLines.length > 2? errLines[errLines.length - 2] : err.message),
			severity: "error",
			lineNumber: parseInt(lineNumber),
			characterOffset: parseInt(charOffset),
			lineContent: (lineNumber > 0 && lines.length >= lineNumber ? lines[lineNumber - 1] : "Unknown line"),
			source: input
		};
	}
})();
