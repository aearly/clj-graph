target/generated/cljs/graph/core.cljs: src/graph/core.cljx
	lein cljx once

dist/graph.js: target/generated/cljs/graph/core.cljs src-cljs/graph.cljs
	lein cljsbuild once

dist/graph.common.js: dist/graph.js
	cat support/preamble.js dist/graph.js support/suffix.js > dist/graph.common.js

default: dist/graph.common.js

clean:
	rm dist/graph.common.js

.PHONY: clean
