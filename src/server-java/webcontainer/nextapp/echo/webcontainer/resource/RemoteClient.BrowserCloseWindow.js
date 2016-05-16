/**
 * Command execution peer: Browser Close Window
 */
Echo.RemoteClient.CommandExec.BrowserCloseWindow = Core.extend(Echo.RemoteClient.CommandExec, {
    
    $static: {
        
        /** @see Echo.RemoteClient.CommandExecProcessor#execute */
        execute: function(client, commandData) {
            client.windowClosing = true;
            window.close();
        }
     },
     
     $load: function() {
        Echo.RemoteClient.CommandExecProcessor.registerPeer("nextapp.echo.app.command.BrowserCloseWindowCommand", this);
     }
});

