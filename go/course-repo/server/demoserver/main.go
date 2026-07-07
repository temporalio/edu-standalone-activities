// Command demoserver is a minimal static file server that hosts the
// standalone-activities heartbeat/demo assets used by the Instruqt track.
//
// It mirrors the TypeScript reference implementation
// (typescript/course-repo/server/demoServer.ts) but is written using only
// the Go standard library so the sandbox image does not need a Node
// runtime.
package main

import (
	"log"
	"net/http"
	"os"
	"path"
	"path/filepath"
	"strings"
)

var mimeTypes = map[string]string{
	".html": "text/html; charset=utf-8",
	".js":   "application/javascript",
	".css":  "text/css",
	".png":  "image/png",
	".svg":  "image/svg+xml",
}

func demosDir() string {
	if dir := os.Getenv("DEMOS_DIR"); dir != "" {
		return dir
	}
	return "/opt/workshop/demos"
}

func handler(baseDir string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		rel := "heartbeat-demo/index.html"
		if r.URL.Path != "/" {
			rel = strings.TrimPrefix(r.URL.Path, "/")
		}

		// Clean the requested path and ensure it stays within baseDir.
		cleanRel := path.Clean("/" + rel)
		filePath := filepath.Join(baseDir, cleanRel)

		absBase, err := filepath.Abs(baseDir)
		if err != nil {
			http.NotFound(w, r)
			return
		}
		absFile, err := filepath.Abs(filePath)
		if err != nil || (absFile != absBase && !strings.HasPrefix(absFile, absBase+string(filepath.Separator))) {
			http.NotFound(w, r)
			return
		}

		info, err := os.Stat(absFile)
		if err != nil || !info.Mode().IsRegular() {
			http.NotFound(w, r)
			return
		}

		ext := filepath.Ext(absFile)
		contentType := mimeTypes[ext]
		if contentType == "" {
			contentType = "text/plain"
		}

		w.Header().Set("Content-Type", contentType)
		w.Header().Set("X-Frame-Options", "ALLOWALL")
		w.Header().Set("Access-Control-Allow-Origin", "*")

		f, err := os.Open(absFile)
		if err != nil {
			return
		}
		defer f.Close()
		http.ServeContent(w, r, "", info.ModTime(), f)
	}
}

func main() {
	baseDir := demosDir()
	http.HandleFunc("/", handler(baseDir))

	const port = ":9001"
	log.Println("Demo server listening on " + port)
	if err := http.ListenAndServe("0.0.0.0"+port, nil); err != nil {
		log.Fatal(err)
	}
}
