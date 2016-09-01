package gmailfs.base;

import gmailfs.framework.Filter;

public interface FilterDialogListener {
    public void onCreateFilter( Filter filter );
    public void onEditFilter( Filter old, Filter newFilter );
}
