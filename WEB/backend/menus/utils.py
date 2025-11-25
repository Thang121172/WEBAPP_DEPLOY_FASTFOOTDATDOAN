from math import radians, sin, cos, atan2, sqrt


def haversine_distance(lat1, lon1, lat2, lon2):
    """
    Tính khoảng cách giữa hai điểm trên Trái Đất bằng công thức Haversine.
    Trả về khoảng cách tính bằng km.
    
    Args:
        lat1, lon1: Vĩ độ và kinh độ của điểm 1 (độ)
        lat2, lon2: Vĩ độ và kinh độ của điểm 2 (độ)
    
    Returns:
        Khoảng cách tính bằng km (float)
    """
    # Bán kính Trái Đất (km)
    R = 6371.0
    
    # Chuyển đổi độ sang radian
    lat1_rad = radians(float(lat1))
    lon1_rad = radians(float(lon1))
    lat2_rad = radians(float(lat2))
    lon2_rad = radians(float(lon2))
    
    # Tính chênh lệch
    dlat = lat2_rad - lat1_rad
    dlon = lon2_rad - lon1_rad
    
    # Công thức Haversine
    a = sin(dlat / 2)**2 + cos(lat1_rad) * cos(lat2_rad) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    distance = R * c
    return distance

