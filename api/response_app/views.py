from django.http import FileResponse, HttpResponse
import os
# Create your views here.

def download(request):
    file_path = os.path.join('media/', 'file.pdf')
    return FileResponse(open(file_path, 'rb'), as_attachment=True, filename='file.pdf')

def index(request):
    return HttpResponse("<h1>Hello, world. You're at the response_app index.</h1>")