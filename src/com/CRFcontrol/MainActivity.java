package com.CRFcontrol;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.lang.String;
import java.lang.Runtime;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import com.jcraft.jsch.*;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.view.View;
import android.text.method.ScrollingMovementMethod;

public class MainActivity extends Activity {
        
    int counter = 0;
    int exitValue = 0;
    boolean errorPossibility = true;
    String currentMode = "status";
    String line = " to be filled ";
    String output = " no response yet ";
    String DAQmachineLogin = "ssh etpdaq@gar-ex-etp04atl";
    String SlowControlMachineLogin = "ssh etpdaq@etpsc01";
    String placeHolder = "TO_BE_CHANGED";
    Process process;
    
    HashMap< String , String > commandDictionary = 
        new HashMap< String , String >(){{
        
        put( "status"  , 
                            DAQmachineLogin+
                            " \" "+
                                " cd /data0/home/etpdaq/caen "+
                                " && "+
                                " echo -n \" ANODES : \" "+
                                " && "+
                                " ./moduleHV -p 580 "+
                                " && "+
                                " echo -n \" CATHODES : \" "+
                                " && "+
                                " ./moduleHV -n 300 "+
                                " && "+
                                " echo \" MEASUREMENT : \" "+
                                " && "+
                                " echo "+
                                    "$( "+
                                        " cd /project/etpdaq3/CRF_data/MM_data"+
                                        " && "+
                                        "ls -tlhd *_2.root "+
                                        " | "+
                                        "head -1 "+
                                    " )"+
                                " && "+
                                " echo -n \' -> # PARSERS :\' "+
                                " && "+
                                " ps fx | grep \'./parser -d \' | wc -l"+
                                " && "+
                                " echo -n \' -> # SRU_DUMPS :\' "+
                                " && "+
                                " ps fx | grep \'./sru_dump -m \' | wc -l"+
                            " \" "+
                            " && "+
                            " echo -n \" SLOWCONTROL : \" "+
                            " && "+
                            SlowControlMachineLogin+
                            " \" "+
                                " source ~/bin/mts_arch "+
                                " > /dev/null "+
                                " && "+
                                " slowcontrol status "+
                                " && "+
                                " ls -lh /export/data/etpsc01/SlowControl/data/slow.dat "+
                            " \" "
                        );
        put( "anode"   , 
                            DAQmachineLogin+
                            " \" "+
                                " cd /data0/home/etpdaq/caen "+
                                " && "+
                                " ./moduleHV -a "+
                                placeHolder+
                            " \" "
                        );
        put( "cathode" ,  
                            DAQmachineLogin+
                            " \" "+
                                " cd /data0/home/etpdaq/caen "+
                                " && "+
                                " ./moduleHV -c "+
                                placeHolder+
                            " \" " 
                        );
        put( "start"   ,  
                            DAQmachineLogin+
                            " \" "+
                                " screen -S runControl -X stuff $\'\\003\' "+
                                " && "+
                                " sleep 2s "+
                                " && "+
                                " screen -S runControl -X stuff "+
                                " \'( "+
                                    " cd /data0/home/etpdaq/etpdaqsvn/CRFscripts "+
                                    " && "+
                                    " ./moduleSTART.sh 3 "+
                                    placeHolder+
                                " )^M\' "+
                            " \" " 
                        );
        put( "stop"    ,  
                            DAQmachineLogin+
                            " \" "+
                                " screen -S runControl -X stuff $\'\\003\' "+
                                " && "+
                                " sleep 2s "+
                                " && "+
                                " screen -S runControl -X stuff "+
                                " \'( "+
                                    " cd /data0/home/etpdaq/etpdaqsvn/CRFscripts "+
                                    " && "+
                                    " ./moduleSTOP.sh "+
                                " )^M\' "+
                            " \" " 
                        );
        put( "reset"   , 
                            SlowControlMachineLogin+
                            " \" "+
                                " source ~/bin/mts_arch "+
                                " > /dev/null "+
                                " && "+
                                " slowcontrol stop "+
                                " && "+
                                " sleep 2s "+
                                " && "+
                                " slowcontrol start "+
                            " \" " 
                        );
        
    }};
    
//     ArrayAdapter<String> commandAdapter = new ArrayAdapter<String>( 
//                                                                 this , 
//                                                                 android.R.layout.simple_spinner_dropdown_item ,
//                                                                 new ArrayList<String>(
//                                                                     commandDictionary.keySet() )
//                                                             );
    
    JSch javaSecureChannel;
    Session session;
    ChannelExec channel;
    
    java.util.Properties config = new java.util.Properties();
    
    BufferedReader stdout;
    BufferedReader stderr;
    
    boolean success = false;
    int TIMEOUT = 30000;
    String host = "gar-sv-login01.garching.physik.uni-muenchen.de";
    Integer port = 22;
    String command = "touch veryLongUselessName.datei";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
    
        final ArrayAdapter<String> commandAdapter = new ArrayAdapter<String>( 
                                                                    this , 
                                                                    android.R.layout.simple_spinner_dropdown_item ,
                                                                    new ArrayList<String>(
                                                                        commandDictionary.keySet() )
                                                                );
        
        final Spinner commandSpinner = (Spinner)findViewById(R.id.commandSpinner); 
        
        final HashMap< String , EditText > inputTexts = 
            new HashMap< String , EditText >(){{
        
            put( "username" , (EditText)findViewById(R.id.usernameInput ) );
            put( "password" , (EditText)findViewById(R.id.passwordInput ) );
            put( "anode"    , (EditText)findViewById(R.id.anodeInput    ) );
            put( "cathode"  , (EditText)findViewById(R.id.cathodeInput  ) );
            put( "start"    , (EditText)findViewById(R.id.startInput    ) );
        
        }};
        
        final ToggleButton activateButton = (ToggleButton)findViewById(R.id.activate);
        final Button sendButton = (Button)findViewById(R.id.send);

        final TextView message  = (TextView)findViewById(R.id.message  );
        final TextView recieved = (TextView)findViewById(R.id.recieved );
        recieved.setMovementMethod(new ScrollingMovementMethod());    
        
        commandSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View v, int position, long id) {
                currentMode = commandAdapter.getItem(position).toString();
                if( activateButton.isChecked() ){ 
                    message.setText( 
                                        " ACTIVE : " 
                                        +
                                        currentMode
                                    );
                    recieved.setText( " no response yet " );
                }
                else{ 
                    message.setText( " not activated " );
                    recieved.setText( " no response yet " );
                }
            }
            @Override
            public void onNothingSelected(AdapterView parent) {
                commandSpinner.setSelection( commandAdapter.getPosition( "status" ) );
                currentMode = "status";
            }
        });                                                     
        commandSpinner.setAdapter( commandAdapter );

        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( activateButton.isChecked() ){ 
                    sendButton.setClickable(true);
                    message.setText( 
                                        " ACTIVE : " 
                                        +
                                        currentMode
                                    );
                }
                else{ 
                    sendButton.setClickable(false);
                    message.setText( " not activated " );
                    recieved.setText( " no response yet " );
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            
                sendButton.setClickable(false);
                
                if( activateButton.isChecked() ){
                
                    activateButton.setChecked(false);
                    message.setText( 
                                        " REQUESTING : "
                                        +
                                        currentMode
                                    );
                                   
                    output = "";
                           
                    success = false;
                    session = null;
                    
                    try {
                    
                        javaSecureChannel = new JSch();
                    
                        session = javaSecureChannel.getSession( 
                                                                    inputTexts
                                                                        .get( "username" )
                                                                        .getText()
                                                                        .toString() , 
                                                                    host , 
                                                                    port 
                                                                );

                        config.put("StrictHostKeyChecking", "no");
                        session.setConfig(config);

                        session.setConfig( new Properties() );
                        session.setUserInfo( null );
                        
                        session.setPassword( 
                                                inputTexts
                                                    .get( "password" )
                                                    .getText()
                                                    .toString() 
                                            );

                        session.connect(TIMEOUT);
                        
                        success = true;
                        
                    } catch (JSchException secureChannelException) {
                        output += " EXCEPTION : at session connection ";
                        output += "\n";
                        output += secureChannelException.getLocalizedMessage().toString();
                    }
                    
                    if( success ){
                    
                        ChannelExec channel = null;
                        
                        try {
                        
                            channel = (ChannelExec) session.openChannel("exec");
                            
                            command = commandDictionary.get( currentMode );
                            
                            if( 
                                currentMode.equals( "anode" ) 
                                ||
                                currentMode.equals( "cathode" ) 
                                ||
                                currentMode.equals( "start" ) 
                            ){
                                command = command.replace( 
                                                            placeHolder ,  
                                                            inputTexts
                                                                .get( currentMode )
                                                                .getText()
                                                                .toString()
                                                        );
                            }
                            
                            channel.setCommand( command );
                            
//                             output += "\n COMMAND \n";
//                             output += command;
//                             output += "\n COM-END \n";

                            stdout = new BufferedReader( 
                                        new InputStreamReader( channel.getInputStream() ) );
                            stderr = new BufferedReader( 
                                        new InputStreamReader( channel.getErrStream() ) );

                            channel.connect(TIMEOUT);
                            success = true;
                        
                            while ((line = stdout.readLine()) != null) {
                                output += line;
                                output += "\n";
                            }
                            
                            while ((line = stderr.readLine()) != null) {
                                output += line;
                                output += "\n";
                            }
                            
                        } catch (IOException | JSchException secureChannelException) {
                        
                            if( session != null ) session.disconnect();
                            output = " EXCEPTION : at channel connection ";
                            output += "\n";
                            output += secureChannelException.getLocalizedMessage().toString();
                            
                        } finally {
                        
                            if( channel != null ) channel.disconnect();
                            if( session != null ) session.disconnect();
                            
                        }
                    
                    }
                    
                    recieved.setText( output );
                    
                }
            }
        });

    }

//     @Override
//     public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
//         currentMode = parent.get(position).toString();
//     }
// 
//     @Override
//     public void onNothingSelected(AdapterView<?> parent) {
//     // TODO Auto-generated method stub
//     }

}
            
////////////////////////////////////////// JAVA-Commandline-Interface /////////////////////////
//                     try {
//                     
//                         process = Runtime.getRuntime().exec( 
// //                                         commandDictionary.get( currentMode ) );
//                                         inputTexts.get( "start" ).getText().toString() );
//                     
//                         reader = new BufferedReader( 
//                                         new InputStreamReader( process.getInputStream() ) );
//                                         
//                         errorReader = new BufferedReader(
//                                         new InputStreamReader( process.getErrorStream() ) );
//                                     
//                         output = "";    
//                         
//                         try {
//                             exitValue = process.waitFor();
//                             if ( exitValue != 0 ) 
//                                 output = " ERROR : abnormal termination \n";
//                         }
//                         catch( Exception e ){
//                             output = " ERROR : can not wait for \n";
//                         }
//                             
// //                         output = currentMode;
// //                         output += " -> count : ";
// //                         counter = 0;
//                         
//                         while ((line = errorReader.readLine()) != null) {
//                             output += line;
//                             output += "\n";
// //                             counter++;
//                         }
//                         
//                         while ((line = reader.readLine()) != null) {
//                             output += line;
//                             output += "\n";
// //                             counter++;
//                         }
// //                         output += counter;
//                     
//                         reader.close();
//                     
//                     } catch (IOException e) {
//                         output = " PROBLEM running command ";
//                     }
////////////////////////////////////////// JAVA-Commandline-Interface END ///////////////////
