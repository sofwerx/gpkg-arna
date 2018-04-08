package net.cognitics.navapp;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CustomGraphics extends View
{


    float bearing=-1;
    float pitch;
    public Context context;
    FrameLayout camera;
    private Activity mainActivity;
    //private ArrayList<NavPoint> navList=new ArrayList<NavPoint>();
    private HashMap<String,NavPoint> navMap=new HashMap<String,NavPoint>();

    public CustomGraphics(Context con)
    {
        super(con);
        this.context=con;
    }

    public CustomGraphics(Context con, AttributeSet set)
    {
        super(con, set);
    }

    public CustomGraphics(Context con, AttributeSet set, int style)
    {
        super(con, set, style);
    }

    @Override
    protected void onDraw(Canvas c)
    {
    }


    public void updatePositions(){
        int width = camera.getMeasuredWidth();
        int height = camera.getMeasuredHeight();
        for (NavPoint n : navMap.values()){

            //Set X and Y to center of screen
            int tX = (width/2)-(n.getWidth()/2);
            int tY = (height/2)-(n.getHeight()/2);
            TextView text = n.getText();
            text.setVisibility(View.GONE);
            text.measure(0, 0);
            boolean showText = true;
            //Offset X and Y based on deltaPitch and deltaBearing
            if (Math.abs(n.getBearing()-bearing)>180){
                if (bearing>180) {
                    //Bearing around 330, target around 30, should be a positive result(60?)
                    tX += 12 * (Math.abs(bearing-360) + (n.getBearing()));
                }else{
                    //Bearing around 30, target around 330, should be negative result(-60?)
                    tX -= 12 * (bearing + Math.abs((n.getBearing())-360));
                }
            }else {
                tX += 12 * ((n.getBearing()) - (bearing));
            }

            int spread = 30; //Hardcoded currently, should be set as a representation of the spread of the route's distance
            int pRatio = ((spread*(1000/((n.getDistance())+1)))-(spread*2));
            if (pRatio>120){
                pRatio=120;
            }

            tY-=16*(n.getPitch()-(pitch))-pRatio;


            //Keep on screen if out of screen
            if (tY<0){
                //CASE: Off screen up
                tY=0;
                showText=false;
            }else if (tY>height-n.getHeight()){
                //CASE: Off screen down
                tY=height-(n.getHeight());
                showText=false;
            }
            if (tX<0){
                //CASE: Off screen to left
                tX=0;
                showText=false;
            }else if (tX>width-n.getWidth()){
                //CASE: Off screen to right
                tX=width-(n.getWidth());
                showText=false;
            }

            //Apply X and Y
            if (showText){
                text.setVisibility(View.VISIBLE);
                text.setX((float)tX-(text.getMeasuredWidth()/4));
                text.setY((float)tY+70);
            }
            n.setX((float)tX);
            n.setY((float)tY);
        }

    }


    public void setViewModel(float bearing, float pitch){
        this.bearing=bearing;
        this.pitch=pitch;
    }

    public void addPoint(FrameLayout camera, float bearing, float pitch, String id)
    {
        addPoint(camera,bearing,pitch,id,0,null);
    }

    public void addPoint(FrameLayout camera, float bearing, float pitch, String id, float distance, String title){

        if(navMap.containsKey(id))
        {
            if(title!=null)
                navMap.get(id).getText().setText(String.format("%s:\n %.3fkm",title,distance/1000.0));
                navMap.get(id).setDistance((int)distance);
            updatePoint(bearing,pitch,id);
            return;
        }
        this.camera=camera;
        final NavPoint newPoint = new NavPoint(context,bearing,pitch);
        newPoint.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_name));
        if(title!=null && title=="*")
            newPoint.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_name));
        else
            newPoint.setBackgroundDrawable(getResources().getDrawable(R.drawable.cnpicon));

        newPoint.setOnClickListener(new AppCompatImageButton.OnClickListener() {
                                        public void onClick(View v) {
                                            PopupMenu popupMenu = new PopupMenu(context,newPoint);
                                            popupMenu.getMenuInflater().inflate(R.menu.navpoint_select,popupMenu.getMenu());
                                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                public boolean onMenuItemClick(MenuItem item) {
                                                    switch (item.getItemId()) {
                                                        case R.id.snap:
                                                            String file = "Picturetest.jpg";
                                                            File newfile = new File(file);
                                                            try {
                                                                newfile.createNewFile();
                                                            }
                                                            catch (IOException e)
                                                            {
                                                            }

                                                            Uri outputFileUri = Uri.fromFile(newfile);

                                                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

                                                            ((Activity)context).startActivityForResult(cameraIntent, 0);
                                                            break;

                                                        case R.id.select:
                                                            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                                            // Start the Intent
                                                            ((Activity)context).startActivityForResult(galleryIntent, 0);
                                                            break;


                                                        default:
                                                            break;

                                                    }
                                                    return true;
                                                }
                                            });
                                            popupMenu.show();
                                        }
                                    });

        navMap.put(id,newPoint);
        camera.addView(newPoint);
        newPoint.setDistance((int)distance);
        camera.addView(newPoint.getText());
        if(title!=null)
            navMap.get(id).getText().setText(String.format("%s:\n %.3fkm",title,distance/1000.0));
    }

    public void updatePoint(float bearing, float pitch, String id)
    {
        if(navMap.containsKey(id))
        {
           navMap.get(id).setBearing(bearing);
           navMap.get(id).setPitch(pitch);
        }
    }
    public void clearPoints(){
        for(View v : navMap.values())
        {
            camera.removeView(v);
        }
        navMap.clear();
    }


}