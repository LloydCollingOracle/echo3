/**
 * Command execution peer: Open Echo3 Window
 */
Echo.RemoteClient.CommandExec.OpenEcho3Window = Core.extend(Echo.RemoteClient.CommandExec, {
    
    $static: {
        
        /** @see Echo.RemoteClient.CommandExecProcessor#execute */
        execute: function(client, commandData) {
            if (!commandData.uri) {
                throw new Error("URI not specified in OpenEcho3WindowCommand.");
            }
            
            window.open(commandData.uri, "_blank");
        }
     },
     
     $load: function() {
        Echo.RemoteClient.CommandExecProcessor.registerPeer("nextapp.echo.app.command.OpenEcho3WindowCommand", this);
     }
});

