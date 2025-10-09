from django.http import FileResponse, HttpResponse, JsonResponse
import os
# Create your views here.

def download(request):

    unique_text = request.GET.get('unique_text')
    file_number = int(request.GET.get('file_number'))


    folder_path = os.path.join('media/', unique_text)
    files = os.listdir(folder_path)
    
    filename = files[file_number]
    print(filename)
    file_path = os.path.join(folder_path, filename)

    print(file_path)

    if not os.path.exists(file_path):
        return HttpResponse("File not found.", status=404)
    
    return FileResponse(open(file_path, 'rb'), as_attachment=True, filename=filename)

def index(request):
    return HttpResponse("<h1>Hello, world. You're at the response app index</h1>")

def get_file_count(request):
    unique_text = request.GET.get('unique_text')
    file_path = os.path.join(f'media/{unique_text}')
    
    if not os.path.exists(file_path):
        return HttpResponse("0")
    
    files = os.listdir(file_path)
    file_count = len(files)

    response = {
        'status': 'success',
        'file_count': file_count,
    }

    return JsonResponse(response)
