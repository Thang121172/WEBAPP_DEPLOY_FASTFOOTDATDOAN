# Generated migration for adding latitude and longitude to Merchant

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('menus', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='merchant',
            name='latitude',
            field=models.DecimalField(blank=True, decimal_places=6, help_text='Vĩ độ của cửa hàng', max_digits=9, null=True),
        ),
        migrations.AddField(
            model_name='merchant',
            name='longitude',
            field=models.DecimalField(blank=True, decimal_places=6, help_text='Kinh độ của cửa hàng', max_digits=9, null=True),
        ),
    ]

