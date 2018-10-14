package com.google.ar.sceneform.samples.hellosceneform.wayfairapi.request;

import com.google.ar.sceneform.samples.hellosceneform.wayfairapi.response.ProductInfoSchema;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface WayfairApiRequest {

    @GET("3dapi/models")
    default Call<List<ProductInfoSchema>> listModels() {
        return null;
    }

}