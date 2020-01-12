package com.contoso.facetutorial;

// <snippet_imports>
import java.io.*;
import java.lang.Object.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;

import com.microsoft.azure.cognitiveservices.vision.faceapi.*;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

// </snippet_imports>

// <snippet_face_imports>
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
// </snippet_face_imports>

public class MainActivity extends Activity {
    // <snippet_mainactivity_fields>
    // Add your Face endpoint to your environment variables.
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    // Add your Face subscription key to your environment variables.
    private final String subscriptionKey = "0e3f77889d214f1a8785c2a4170322a4";

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    // </snippet_mainactivity_fields>

    // For Detect Faces and Find Similar Faces examples
    // This image should have a single face.
    final String SINGLE_FACE_URL = "https://www.biography.com/.image/t_share/MTQ1MzAyNzYzOTgxNTE0NTEz/john-f-kennedy---mini-biography.jpg";
    final String SINGLE_IMAGE_NAME =
            SINGLE_FACE_URL.substring(SINGLE_FACE_URL.lastIndexOf('/')+1, SINGLE_FACE_URL.length());
    // This image should have several faces. At least one should be similar to the face in singleFaceImage.
    final String  GROUP_FACES_URL = "http://www.historyplace.com/kennedy/president-family-portrait-closeup.jpg";
    final String GROUP_IMAGE_NAME =
            GROUP_FACES_URL.substring(GROUP_FACES_URL.lastIndexOf('/')+1, GROUP_FACES_URL.length());

    // <snippet_mainactivity_methods>
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(
                        intent, "Select Picture"), PICK_IMAGE);
            }
        });

        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                // Comment out for tutorial
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // </snippet_mainactivity_methods>
    }

    // <snippet_detection_methods>
    // Detect faces by uploading a face image.
    // Frame faces after detection.
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                   null          // returnFaceAttributes:
                                    /* new FaceServiceClient.FaceAttributeType[] {
                                        FaceServiceClient.FaceAttributeType.Age,
                                        FaceServiceClient.FaceAttributeType.Gender }
                                    */
                            );
                            if (result == null){
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        if(!exceptionMessage.equals("")){
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        ImageView imageView = findViewById(R.id.imageView1);
                        imageView.setImageBitmap(
                                drawFaceRectanglesOnBitmap(imageBitmap, result));
                        imageBitmap.recycle();
                    }
                };

        detectTask.execute(inputStream);

    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }
    // </snippet_detection_methods>

    // <snippet_drawrectangles>
    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
            Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(10);
            if (faces != null) {
                for (Face face : faces) {
                    FaceRectangle faceRectangle = face.faceRectangle;
                    canvas.drawRect(
                            faceRectangle.left,
                            faceRectangle.top,
                            faceRectangle.left + faceRectangle.width,
                            faceRectangle.top + faceRectangle.height,
                            paint);
                }
            }
            return bitmap;
    }
    // </snippet_drawrectangles>

    /**
     * Find Similar
     * Finds a similar face in another image with 2 lists of face IDs.
     * Returns the IDs of those that are similar.
     */
    public static List<UUID> findSimilar(FaceAPI client, List<UUID> singleFaceList, List<UUID> groupFacesList, String groupImageName) {
        // With our list of the single-faced image ID and the list of group IDs, check if any similar faces.
        List<SimilarFace> listSimilars = client.faces().findSimilar(singleFaceList.get(0),
                new FindSimilarOptionalParameter().withFaceIds(groupFacesList));
        // Display the similar faces found
        System.out.println();
        System.out.println("Similar faces found in group photo " + groupImageName + " are:");
        // Create a list of UUIDs to hold the similar faces found
        List<UUID> similarUuids = new ArrayList<>();
        for (SimilarFace face : listSimilars) {
            similarUuids.add(face.faceId());
            System.out.println("Face ID: " + face.faceId());
            // Get and print the level of certainty that there is a match
            // Confidence range is 0.0 to 1.0. Closer to 1.0 is more confident
            System.out.println("Confidence: " + face.confidence());
        }
        System.out.println();

        return similarUuids;
    }
    /**
     * END - Find Similar
     */

}