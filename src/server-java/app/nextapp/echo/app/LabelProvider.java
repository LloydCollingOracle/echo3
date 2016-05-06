package nextapp.echo.app;

import nextapp.echo.app.Component;

/**
 * Interface for entities that implement BoundField and should have a label.
 */
public interface LabelProvider {

    void setLabel(Component c);

    Component getLabel();
}
