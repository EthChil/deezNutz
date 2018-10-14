/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.hellosceneform.REST.GrabShoe;
import com.google.ar.sceneform.samples.hellosceneform.REST.Product;
import com.google.ar.sceneform.samples.hellosceneform.REST.ShoesResponse;
import com.google.ar.sceneform.samples.hellosceneform.REST.StockXService;
import com.google.ar.sceneform.samples.hellosceneform.wayfairapi.RetrofitClientInstance;
import com.google.ar.sceneform.samples.hellosceneform.wayfairapi.request.WayfairApiRequest;
import com.google.ar.sceneform.samples.hellosceneform.wayfairapi.response.ProductInfoSchema;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.loopj.android.http.AsyncHttpClient.LOG_TAG;


/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private static final String EMAIL = "ethanchilderhose@gmail.com";
  private static final String API_KEY = "5bc16e8b0607a";

  private ArFragment arFragment;
  private ViewRenderable imgRenderable;
  private ModelRenderable modelRenderable;
  private ModelRenderable cubeRenderable;
  private ArrayList<Anchor> anchors;
  private enum ProductType {
      WAYFAIR,
      SHOE,
      SHIRT
  }
  private ProductType productType = ProductType.WAYFAIR;
  private List<ProductInfoSchema> wayfair;
  //Handle shoes
//  private GrabShoe shoeMaster = new GrabShoe();
//  private Shoe allShoes[] = shoeMaster.getShoeArr();

  //Counter bois

    Retrofit retrofit;
    private int itemNumber = 0;
    private ArrayList<Shoe> shoes = new ArrayList<Shoe>();
    private ArrayList<Shoe> tees = new ArrayList<Shoe>();

  public void setupRetrofit() {
      retrofit = new Retrofit.Builder()
              .baseUrl("https://gateway.stockx.com/public/v1/")
              .addConverterFactory(GsonConverterFactory.create())
              .build();
  }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }


    public void onClear() {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }

        }
    }

    @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    HelloSceneformActivity self = this;

    setupRetrofit();
    createShoeService();

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);


        // Make Wayfair Api call
    // API DOCS: bit.ly/wayfair3dapi
    // Get 5 models without registration / key
    // Register for free to get access to 200 Wayfair models
    RetrofitClientInstance.getRetrofitInstance(EMAIL, API_KEY)
            .create(WayfairApiRequest.class)
            .listModels()
            .enqueue(new Callback<List<ProductInfoSchema>>() {
                @Override
                public void onResponse(Call<List<ProductInfoSchema>> call, Response<List<ProductInfoSchema>> response) {
                    // Step 2: Get 3D assets and create a renderable
                    wayfair = response.body();
                    pullIntoView();
                }

                @Override
                public void onFailure(Call<List<ProductInfoSchema>> call, Throwable t) {
                    Log.e("Api request failed", t.getMessage());
                }
            });


    arFragment.setOnTapArPlaneListener(
      (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
        if(productType != ProductType.WAYFAIR){

            // Create the Anchor.
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setName("left top");
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            //Hardcoded andhors
            // Create the transformable andy and add it to the anchor.
            TransformableNode img = new TransformableNode(arFragment.getTransformationSystem());
            img.getScaleController().setMinScale(0.01f);
            img.getScaleController().setMaxScale(2.0f);
            img.setLocalScale(new Vector3(2f, 2f, 2f));
            img.setParent(anchorNode);
            img.setRenderable(imgRenderable);

            // Change the rotation
            if (plane.getType() ==  Plane.Type.VERTICAL) {
                float[] yAxis = plane.getCenterPose().getYAxis();
                Vector3 planeNormal = new Vector3(yAxis[0], yAxis[1], yAxis[2]);
                Quaternion upQuat = Quaternion.lookRotation(planeNormal, Vector3.up());
                img.setWorldRotation(upQuat);
            }

            img.select();

              } else {

                if (modelRenderable == null) {
                    return;
                }

                // Step 3: Build the Scene
                // Create the Anchor.
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                // Create the transformable model Node and add it to the anchor.
                // A TransformableNode allows the node to be Translated, Rotated and Scaled by user
                TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                modelNode.setParent(anchorNode);
                modelNode.setRenderable(modelRenderable);
                // Wayfair models are of correct scale and dimensions already
                // Disable Scale controller of a TransformableNode in order to prevent model scaling
                modelNode.getScaleController().setEnabled(false);
                modelNode.select();

                // Inorder to hide the built in shadow plane for Wayfair models
                // This is not required for other models
                modelRenderable.setMaterial(modelRenderable.getSubmeshCount() - 1, cubeRenderable.getMaterial());
              }
      });

       ImageView middleImg = findViewById(R.id.middle_img);
       TextView imgText = findViewById(R.id.item_text);

       // TODO: REPLACE

        if (self.shoes.size() > 0) {
            pullIntoView();
        } else {
            Log.i("FOOBAR", self.shoes.toString());
        }
//        for(int i = 0; i < 25; i++){
//            if(shoes.get(i) == null){
//                Log.i("policemenaregay", "ethan succs"+Integer.toString(i));
//            }
//        }
      Button leftButton = findViewById(R.id.left_button);
      leftButton.setOnClickListener(
              (unusedView) -> {
                  itemNumber --;
                  if(itemNumber < 0){
                      itemNumber = 23;
                  }
                 pullIntoView();

              });
      // Initialize the "right" button.
      Button rightButton = findViewById(R.id.right_button);
      rightButton.setOnClickListener((unused) -> {
          itemNumber ++;
          if(itemNumber > 23){
              itemNumber = 0;
          }
          pullIntoView();
      });
      Button deletebutton = findViewById(R.id.delete_button);
      deletebutton.setOnClickListener(
          (unused) -> {
              onClear();
          });

        Button cyclebutton = findViewById(R.id.cycle_button);
        cyclebutton.setOnClickListener(
                (unused) -> {
                    productType = ProductType.values()[(productType.ordinal() + 1) % ProductType.values().length];
                    itemNumber = 0;
                    pullIntoView();
                });
  }

  public void pullIntoView() {
      ImageView middleImg = findViewById(R.id.middle_img);
      TextView imgText = findViewById(R.id.item_text);
        if (productType == ProductType.WAYFAIR) {
            if (wayfair == null) {
                return;
            }
            get3DAsset(wayfair.get(itemNumber).getModel().getGlbUrl());
            Picasso.get().load(wayfair.get(this.itemNumber).getThumbnailImageUrl()).into(middleImg);
            imgText.setText(wayfair.get(itemNumber).getProducName() + "\n Price: $" + wayfair.get(itemNumber).getSalePrice());
        } else {
            if (shoes == null) {
                return;
            }
            imgText.setText(shoes.get(itemNumber).name + "\n Price: $" + Integer.toString(shoes.get(itemNumber).cost));
            Picasso.get().load(this.shoes.get(this.itemNumber).imageLink).into(middleImg);
            ImageView view = (ImageView) getLayoutInflater().inflate(R.layout.test_view, null);

            Picasso.get().load(shoes.get(itemNumber).imageLink).into(view);


            ViewRenderable.builder()
                    .setView(arFragment.getContext(), view)
                    .build()
                    .thenAccept(
                            (renderable) -> {
                                imgRenderable = renderable;


                            });

            Log.i("FOOBAR", this.shoes.toString());
            Log.i("FOOBAR", this.shoes.get(this.itemNumber).imageLink);
        }
  }


  public StockXService createShoeService() {
      StockXService service = retrofit.create(StockXService.class);

      retrofit2.Call<ShoesResponse> call = service.getShoes();
      HelloSceneformActivity self = this;
      call.enqueue(new Callback<ShoesResponse>() {

          @Override
          public void onResponse(retrofit2.Call<ShoesResponse> call,
                                 Response<ShoesResponse> response) {
              List<Product> products = response.body().Products;

              ArrayList shoes = new ArrayList<Shoe>();
              ArrayList tees = new ArrayList<Shoe>();


              for (int i = 0; i < products.size(); i++) {
                  Product product = products.get(i);

                  Shoe shoe = new Shoe(
                          HttpUtils.genImageUrl(product.shortDescription),
                          Integer.parseInt(product.retailPrice),
                          product.shortDescription);
                  shoes.add(shoe);

                  Shoe tee1 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Supreme-Cat-in-the-Hat-Hooded-Sweatshirt-Heather-Grey.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          215,
                          "Supreme Cat In the Hat Hoodie");

                  Shoe tee2 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Supreme-GORE-TEX-Court-Jacket-White.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          478,
                          "Supreme GORE-TEX Court Jacket White");

                  Shoe tee3 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Supreme-Cat-in-the-Hat-Hooded-Sweatshirt-Bright-Royal.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          375,
                          "Supreme Cat In The Hat Hoodie Blue");

                  Shoe tee4 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Nikelab-x-OFF-WHITE-Mercurial-NRG-X-Hoodie-Black.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          579,
                          "Nikelab x OFF-WHITE Mercurial NRG X Hoodie Black");

                  Shoe tee5 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Supreme-Nike-Crewneck-Burgundy.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          149,
                          "Supreme Nike Crewneck Burgundy");

                  Shoe tee6 =  new Shoe(
                          "https://stockx.imgix.net/products/streetwear/Supreme-Group-Tee-White.jpg?fit=fill&bg=FFFFFF&w=1400&h=1000&auto=format,compress&trim=color&q=90",
                          58,
                          "Supreme Group Tee White\n");

                  tees.add(tee1);
                  tees.add(tee2);
                  tees.add(tee3);
                  tees.add(tee4);
                  tees.add(tee5);
                  tees.add(tee6);

              }

              self.shoes = shoes;
              self.pullIntoView();


          }

          @Override
          public void onFailure (Call<ShoesResponse> call, Throwable t){
              Log.i(LOG_TAG, t.getMessage());
          }




      });
      return service;
  }


    // Step 2: Get 3D assets and create a renderable
    private void get3DAsset(String modelUrl) {
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this,
                        RenderableSource.builder().setSource(
                                this,
                                Uri.parse(modelUrl),
                                RenderableSource.SourceType.GLB)
                                .setScale(1.0f)
                                .build())
                .build()
                .thenAccept(renderable -> {
                    // Create a renderable
                    this.modelRenderable = renderable;
                    getCubeAsset();
                })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load model renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    // Inorder to hide the built in shadow plane for Wayfair models
    // This is not required for other models
    private void getCubeAsset() {
        ModelRenderable.builder()
                .setSource(this, R.raw.cube)
                .build()
                .thenAccept(renderable -> {
                    cubeRenderable = renderable;
                })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load cube model renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }
}
