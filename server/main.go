package main

import (
	"fmt"
	"io"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

const (
	DefaultPort    = 8080
	VideoFileName  = "video.mp4"
	MaxUploadSize  = 2 * 1024 * 1024 * 1024
)

var (
	videoDir  string
	videoPath string
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "Usage: server <video_directory>")
		os.Exit(1)
	}

	videoDir = os.Args[1]
	videoPath = filepath.Join(videoDir, VideoFileName)

	port := findAvailablePort(DefaultPort)
	if port == 0 {
		fmt.Fprintln(os.Stderr, "Failed to find available port")
		os.Exit(1)
	}

	ip := getLocalIP()
	if ip == "" {
		fmt.Fprintln(os.Stderr, "Failed to get local IP")
		os.Exit(1)
	}

	http.HandleFunc("/", handleIndex)
	http.HandleFunc("/upload", handleUpload)
	http.HandleFunc("/status", handleStatus)
	http.HandleFunc("/ip", handleIP)

	addr := fmt.Sprintf(":%d", port)
	fmt.Printf("Server starting at http://%s:%d\n", ip, port)
	fmt.Printf("Video directory: %s\n", videoDir)

	if err := http.ListenAndServe(addr, nil); err != nil {
		fmt.Fprintf(os.Stderr, "Server error: %v\n", err)
		os.Exit(1)
	}
}

func findAvailablePort(startPort int) int {
	for port := startPort; port < startPort+100; port++ {
		ln, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
		if err == nil {
			ln.Close()
			return port
		}
	}
	return 0
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return ""
	}

	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				return ipnet.IP.String()
			}
		}
	}
	return ""
}

func handleIndex(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	html := `<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>视频上传</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            width: 100%;
            max-width: 400px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 30px;
            font-size: 24px;
        }
        .upload-area {
            border: 3px dashed #667eea;
            border-radius: 15px;
            padding: 40px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s ease;
            background: #f8f9ff;
        }
        .upload-area:hover {
            border-color: #764ba2;
            background: #f0f2ff;
        }
        .upload-area.dragover {
            border-color: #764ba2;
            background: #e8ebff;
        }
        .upload-icon {
            font-size: 48px;
            margin-bottom: 15px;
        }
        .upload-text {
            color: #666;
            font-size: 16px;
        }
        .file-input { display: none; }
        .file-name {
            margin-top: 15px;
            padding: 10px;
            background: #e8ebff;
            border-radius: 8px;
            color: #333;
            word-break: break-all;
        }
        .progress-container {
            margin-top: 20px;
            display: none;
        }
        .progress-bar {
            width: 100%;
            height: 8px;
            background: #e0e0e0;
            border-radius: 4px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
            width: 0%;
            transition: width 0.3s ease;
        }
        .progress-text {
            text-align: center;
            margin-top: 10px;
            color: #666;
        }
        .submit-btn {
            width: 100%;
            padding: 15px;
            margin-top: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 18px;
            cursor: pointer;
            transition: transform 0.2s ease;
        }
        .submit-btn:hover { transform: scale(1.02); }
        .submit-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        .status {
            margin-top: 20px;
            padding: 15px;
            border-radius: 10px;
            text-align: center;
            display: none;
        }
        .status.success {
            background: #d4edda;
            color: #155724;
            display: block;
        }
        .status.error {
            background: #f8d7da;
            color: #721c24;
            display: block;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📺 视频上传到电视</h1>
        <form id="uploadForm" enctype="multipart/form-data">
            <div class="upload-area" id="dropZone">
                <div class="upload-icon">📁</div>
                <div class="upload-text">点击或拖拽视频文件到这里</div>
                <div class="file-name" id="fileName" style="display:none;"></div>
            </div>
            <input type="file" id="fileInput" name="video" class="file-input" accept="video/*">
            <div class="progress-container" id="progressContainer">
                <div class="progress-bar">
                    <div class="progress-fill" id="progressFill"></div>
                </div>
                <div class="progress-text" id="progressText">0%</div>
            </div>
            <button type="submit" class="submit-btn" id="submitBtn" disabled>开始上传</button>
        </form>
        <div class="status" id="status"></div>
    </div>
    <script>
        const dropZone = document.getElementById('dropZone');
        const fileInput = document.getElementById('fileInput');
        const fileName = document.getElementById('fileName');
        const submitBtn = document.getElementById('submitBtn');
        const uploadForm = document.getElementById('uploadForm');
        const progressContainer = document.getElementById('progressContainer');
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        const status = document.getElementById('status');

        dropZone.addEventListener('click', () => fileInput.click());

        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('dragover');
        });

        dropZone.addEventListener('dragleave', () => {
            dropZone.classList.remove('dragover');
        });

        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                fileInput.files = files;
                updateFileName();
            }
        });

        fileInput.addEventListener('change', updateFileName);

        function updateFileName() {
            if (fileInput.files.length > 0) {
                fileName.textContent = fileInput.files[0].name;
                fileName.style.display = 'block';
                submitBtn.disabled = false;
            }
        }

        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!fileInput.files.length) return;

            const file = fileInput.files[0];
            const formData = new FormData();
            formData.append('video', file);

            submitBtn.disabled = true;
            progressContainer.style.display = 'block';
            status.className = 'status';
            status.textContent = '';

            try {
                const xhr = new XMLHttpRequest();
                xhr.upload.addEventListener('progress', (e) => {
                    if (e.lengthComputable) {
                        const percent = Math.round((e.loaded / e.total) * 100);
                        progressFill.style.width = percent + '%';
                        progressText.textContent = percent + '%';
                    }
                });

                xhr.addEventListener('load', () => {
                    if (xhr.status === 200) {
                        status.className = 'status success';
                        status.textContent = '✅ 上传成功！电视即将开始播放...';
                    } else {
                        status.className = 'status error';
                        status.textContent = '❌ 上传失败: ' + xhr.statusText;
                    }
                    submitBtn.disabled = false;
                });

                xhr.addEventListener('error', () => {
                    status.className = 'status error';
                    status.textContent = '❌ 网络错误，请重试';
                    submitBtn.disabled = false;
                });

                xhr.open('POST', '/upload');
                xhr.send(formData);
            } catch (err) {
                status.className = 'status error';
                status.textContent = '❌ 上传失败: ' + err.message;
                submitBtn.disabled = false;
            }
        });
    </script>
</body>
</html>`
	w.Write([]byte(html))
}

func handleUpload(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, MaxUploadSize)

	if err := r.ParseMultipartForm(32 << 20); err != nil {
		http.Error(w, "Failed to parse form: "+err.Error(), http.StatusBadRequest)
		return
	}

	file, header, err := r.FormFile("video")
	if err != nil {
		http.Error(w, "Failed to get file: "+err.Error(), http.StatusBadRequest)
		return
	}
	defer file.Close()

	if !isValidVideoFile(header.Filename) {
		http.Error(w, "Invalid file type. Only video files are allowed.", http.StatusBadRequest)
		return
	}

	tempPath := videoPath + ".tmp"

	dst, err := os.Create(tempPath)
	if err != nil {
		http.Error(w, "Failed to create file: "+err.Error(), http.StatusInternalServerError)
		return
	}

	written, err := io.Copy(dst, file)
	if err != nil {
		dst.Close()
		os.Remove(tempPath)
		http.Error(w, "Failed to write file: "+err.Error(), http.StatusInternalServerError)
		return
	}
	dst.Close()

	if err := os.Rename(tempPath, videoPath); err != nil {
		os.Remove(tempPath)
		http.Error(w, "Failed to finalize file: "+err.Error(), http.StatusInternalServerError)
		return
	}

	fmt.Printf("Received video: %s (%d bytes)\n", header.Filename, written)
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Upload successful: %d bytes received", written)
}

func handleStatus(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	info, err := os.Stat(videoPath)
	if os.IsNotExist(err) {
		fmt.Fprintf(w, `{"has_video": false}`)
		return
	}

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, `{"has_video": true, "size": %d, "modified": "%s"}`,
		info.Size(), info.ModTime().Format("2006-01-02 15:04:05"))
}

func handleIP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ip := getLocalIP()
	fmt.Fprintf(w, `{"ip": "%s", "port": %d}`, ip, DefaultPort)
}

func isValidVideoFile(filename string) bool {
	ext := strings.ToLower(filepath.Ext(filename))
	validExts := map[string]bool{
		".mp4":  true,
		".mkv":  true,
		".avi":  true,
		".mov":  true,
		".wmv":  true,
		".flv":  true,
		".webm": true,
		".m4v":  true,
		".3gp":  true,
	}
	return validExts[ext]
}
