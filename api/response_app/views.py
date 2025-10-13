from django.http import FileResponse, HttpResponse, JsonResponse
import os
# Create your views here.

def download(request):
    unique_text = request.GET.get('unique_text')
    file_index = int(request.GET.get('file_index'))

    try:
        folder_path = os.path.join('media', unique_text)
        files = os.listdir(folder_path)
        
        filename = files[file_index]
        file_path = os.path.join(folder_path, filename)

    except Exception as e:
        response ={
            'status': 'error',
            'message': 'File not found.' + str(e)
        }
        return JsonResponse(response, status=404)
    
    return FileResponse(open(file_path, 'rb'), as_attachment=True, filename=filename)

def index(request):
    return HttpResponse("<h1>Hello, world. You're at the response app index</h1>")

def get_file_count(request):
    unique_text = request.GET.get('unique_text')
    folder_path = os.path.join('media', unique_text)
    
    if not os.path.exists(folder_path):
        response ={
            'status': 'error',
            'message': 'File not found.'
        }
        return JsonResponse(response, status=404)
    
    files = os.listdir(folder_path)
    file_count = len(files)

    response = {
        'status': 'success',
        'file_count': file_count,
    }
    
    return JsonResponse(response)


def post_files(request):
    if request.method == 'POST':
        unique_text = request.POST.get('unique_text')
        files = request.FILES.getlist('files')

        folder_path = os.path.join('media', unique_text)
        os.makedirs(folder_path, exist_ok=True)

        for file in files:
            file_path = os.path.join(folder_path, file.name)
            with open(file_path, 'wb+') as destination:
                for chunk in file.chunks():
                    destination.write(chunk)

        response = {
            'status': 'success',
            'message': f'{len(files)} files uploaded successfully.'
        }
        return JsonResponse(response)
    else:
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
        }
        return JsonResponse(response, status=400)