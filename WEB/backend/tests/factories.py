# Factory placeholders (factory_boy)


class UserFactory:
    @staticmethod
    def create(**kwargs):
        return {"id": 1, "username": "user"}
