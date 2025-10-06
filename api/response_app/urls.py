from django.urls import path
from . import views

urlpatterns = [
    path('download/', views.download, name='download'),
    path('', views.index, name='index'),
]