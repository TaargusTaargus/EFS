package gmailfs.framework;

import gmailfs.base.File;
import pronus.gmailfs.R;

public class Filter implements File {

    public enum FilterType {

        BIN( R.drawable.bin_icon ),
        FOLDER( R.drawable.folder_icon);

        private Integer icon;

        FilterType( Integer icon ) {
            this.icon = icon;
        }

        public Integer getIconResource() { return icon; }

        public static FilterType stringToFilterType( String type ) {
            for( FilterType val : FilterType.values() ) {
                if( val.toString().equals( type.toUpperCase() ) )
                    return val;
            }
            return null;
        }

    }

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
    private FilterType type;
    private String filterText, filterTitle;

    public Filter( String filter, String title, FilterKey key, FilterType type ) {
        this.filterText = filter;
        this.filterTitle = title;
        this.key = key;
        this.type = type;
    }

    public Integer getIconResource() { return type.getIconResource(); }
    public FilterKey getFilterKey() { return key; }
    public FilterType getFilterType() { return type; }
    public String getFilterText() { return filterText; }
    public String getFileTitle() { return filterTitle; }

}