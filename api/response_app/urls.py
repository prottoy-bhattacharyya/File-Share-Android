from django.urls import path
from . import views

urlpatterns = [
    path('get_file_count/', views.get_file_count, name='get_file_number'),
    path('download/', views.download, name='download'),
    path('', views.index, name='index'),
]