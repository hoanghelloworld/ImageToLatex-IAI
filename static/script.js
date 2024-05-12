document.getElementById("file").addEventListener("change", function() {
    var fileInput = document.getElementById("file");
    var fileName = fileInput.files[0].name;
    document.getElementById("file-name").innerText = fileName;

    // Kiểm tra nếu file là ảnh
    if (/\.(jpe?g|png|gif)$/i.test(fileName)) {
        var reader = new FileReader();

        reader.onload = function(event) {
            var img = document.createElement("img");
            img.src = event.target.result;
            img.classList.add("thumbnail");
            document.getElementById("file-name").appendChild(img);
        };

        reader.readAsDataURL(fileInput.files[0]);
    }
});

// Lấy phần tử mà bạn muốn kéo file vào
var dropzone = document.getElementById("dropzone");

// Thêm sự kiện khi kéo file vào dropzone
dropzone.addEventListener("dragover", function(e) {
    e.stopPropagation();
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
});

// Thêm sự kiện khi thả file
dropzone.addEventListener("drop", function(e) {
    e.stopPropagation();
    e.preventDefault();
    var files = e.dataTransfer.files; // Danh sách các file đã thả

    // Xử lý file thả vào
    var fileInput = files[0];
    var fileName = fileInput.name;
    document.getElementById("file-name").innerText = fileName;

     // Gán file đã thả vào trường input file
     document.getElementById("file").files = files;

    // Kiểm tra nếu file là ảnh
    if (/\.(jpe?g|png|gif)$/i.test(fileName)) {
        var reader = new FileReader();

        reader.onload = function(event) {
            var img = document.createElement("img");
            img.src = event.target.result;
            img.classList.add("thumbnail");
            document.getElementById("file-name").appendChild(img);
        };

        reader.readAsDataURL(fileInput);
    }
});