package gmailfs.framework;

import pronus.gmailfs.R;

public class Filter {

    public enum FilterKey {

        SUBJECT( "Subject" ),
        SENDER( "From" );

        private String keyword;

        FilterKey( String keyword ) {
            this.keyword = keyword;
        }

        public String getHeaderKeyword() { return keyword; }

        public static FilterKey stringToFilterKey(String type ) {
            for( FilterKey val : FilterKey.values() ) {
                if( val.toString().equals( type.toUpperCase() ) )
                    return val;
            }
            return null;
        }

    }

    public final static int BIN_RESOURCE_ICON = R.drawable.bin_icon;
    public final static int FOLDER_RESOURCE_ICON = R.drawable.folder_icon;

    private FilterKey key;
    private String filterText, filterTitle;

    public Filter( String filter, String title, FilterKey key ) {
        this.filterText = filter;
        this.filterTitle = title;
        this.key = key;
    }

    public Integer getIconResource() { return FOLDER_RESOURCE_ICON; }
    public FilterKey getFilterKey() { return key; }
    public String getFilterText() { return filterText; }
    public String getFilterID() { return filterTitle; }

}