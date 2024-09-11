package com.sparta.map.infrastructure.api;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;

import com.sparta.map.domain.model.Coordinates;
import com.sparta.map.domain.repository.MapRepository;
import com.sparta.map.presentation.dto.request.DirectionsRequest;
import com.sparta.map.presentation.dto.response.DirectionsResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsApiService implements MapRepository {
    private final GeoApiContext context;

    // 생성자에서 API 키를 설정하여 구글 맵 API와 통신할 수 있는 컨텍스트를 만듭니다.
    public GoogleMapsApiService(@Value("${google.maps.api.key}") String apiKey) {
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }


    /**
     * Geocoding API를 사용하여 주소를 위도 및 경도로 변환합니다.
     *
     * @param address String - 변환할 주소
     * @return Coordinates - 위도와 경도를 포함한 좌표 객체
     * @throws ApiException, InterruptedException, IOException - API 호출 중 발생할 수 있는 예외
     */
    @Override
    public Coordinates getCoordinates(String address) throws Exception {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
            if (results.length > 0) {
                LatLng location = results[0].geometry.location;
                return new Coordinates(location.lat, location.lng);
            } else {
                throw new IllegalArgumentException("No results found for the given address: " + address);
            }
        } catch (ApiException | InterruptedException | IOException e) {
            throw new RuntimeException("Failed to get coordinates from Google Maps API", e);
        }
    }

    /**
     * Directions API를 사용하여 경로를 검색합니다.
     *
     * @param request DirectionsRequest - 출발지, 목적지, 경유지를 포함하는 경로 요청
     * @return DirectionsResponse - 경로 정보 (요약, 거리, 소요 시간)
     * @throws ApiException, InterruptedException, IOException - API 호출 중 발생할 수 있는 예외
     */
    @Override
    public DirectionsResponse getDirections(DirectionsRequest request) throws Exception {
        TravelMode travelMode = request.getMode() != null ? request.getMode() : TravelMode.TRANSIT;  // 기본값

        DirectionsApiRequest directionsRequest = DirectionsApi.newRequest(context)
                .origin(request.getOrigin())          // 출발지 설정
                .destination(request.getDestination()) // 목적지 설정
                .optimizeWaypoints(true)              // 경유지 최적화 옵션
                .mode(com.google.maps.model.TravelMode.valueOf(travelMode.name()));  // Google Maps의 TravelMode로 변환

        DirectionsResult result = directionsRequest.await();
        DirectionsRoute route = result.routes[0];

        String summary = route.summary;
        if (summary == null || summary.isEmpty()) {
            summary = request.getOrigin() + " -> " + request.getDestination();
        }

        return new DirectionsResponse(
                summary,
                route.legs[0].distance.inMeters,
                route.legs[0].duration.inSeconds
        );
    }

}
