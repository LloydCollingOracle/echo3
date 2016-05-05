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

            var DelayedMessageRunnable = Core.extend(Core.Web.Scheduler.Runnable, {

                _message: null,

                $construct: function(message) {
                    this._message = message;
                },

                run: function() {
                    window.open(this._message, "_blank");
                }
            });
            Core.Web.Scheduler.add(new DelayedMessageRunnable(commandData.uri));
        }
     },
     
     $load: function() {
        Echo.RemoteClient.CommandExecProcessor.registerPeer("nextapp.echo.app.command.OpenEcho3WindowCommand", this);
     }
});

