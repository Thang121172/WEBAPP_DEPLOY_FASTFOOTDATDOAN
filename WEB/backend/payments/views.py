from rest_framework.views import APIView
from rest_framework.response import Response


class CreatePaymentView(APIView):
    def post(self, request):
        return Response({"payment_url": "https://payments.example/"})


class PaymentCallbackView(APIView):
    def post(self, request):
        return Response({"status": "ok"})
