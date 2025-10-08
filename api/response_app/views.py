from django.http import FileResponse, HttpResponse
import os
# Create your views here.

def download(request):
    file_path = os.path.join('media/', 'pic.png')
    if not os.path.exists(file_path):
        return HttpResponse("File not found.", status=404)
    
    return FileResponse(open(file_path, 'rb'), as_attachment=True, filename='pic.png')

def index(request):
    return HttpResponse("<h1>Hello, world. You're at the response app index</h1>")