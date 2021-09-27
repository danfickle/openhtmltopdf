package com.openhtmltopdf.util;

public interface LogMessageId {

    Enum<?> getEnum();
    String getWhere();
    String getMessageFormat();
    String formatMessage(Object[] args);

    enum LogMessageId0Param implements LogMessageId {
        CSS_PARSE_MUST_PROVIDE_AT_LEAST_A_FONT_FAMILY_AND_SRC_IN_FONT_FACE_RULE(XRLog.CSS_PARSE, "Must provide at least a font-family and src in @font-face rule"),

        RENDER_BUG_FONT_DIDNT_CONTAIN_EXPECTED_CHARACTER(XRLog.RENDER, "BUG. Font didn't contain expected character."),
        RENDER_FONT_LIST_IS_EMPTY(XRLog.RENDER, "Font list is empty."),

        LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES(XRLog.LOAD, "Unable to disable XML External Entities, which might put you at risk to XXE attacks"),
        LOAD_COULD_NOT_SET_VALIDATION_NAMESPACE_FEATURES_FOR_XML_PARSER(XRLog.LOAD, "Could not set validation/namespace features for XML parser, exception thrown."),
        LOAD_SAX_PARSER_BY_REQUEST_NOT_CHANGING_PARSER_FEATURES(XRLog.LOAD, "SAX Parser: by request, not changing any parser features."),
        LOAD_PAGE_DOES_NOT_EXIST_FOR_PDF_IN_IMG_TAG(XRLog.LOAD, "Page does not exist for pdf in img tag. Ignoring!"),
        LOAD_EMBEDDED_DATA_URI_MUST_BE_ENCODED_IN_BASE64(XRLog.LOAD, "Embedded data uris must be encoded in base 64."),
        LOAD_FALLING_BACK_ON_THE_DEFAULT_PARSER(XRLog.LOAD, "falling back on the default parser"),
        LOAD_BASE_URL_IS_NULL_TRYING_TO_CONFIGURE_ONE(XRLog.LOAD, "Base url is null, trying to configure one now."),

        RENDER_TRIED_TO_SET_NON_INVERTIBLE_CSS_TRANSFORM(XRLog.RENDER, "Tried to set a non-invertible CSS transform. Ignored."),
        RENDER_LINEAR_GRADIENT_IS_NOT_SUPPORTED(XRLog.RENDER, "linear-gradient(...) is not supported in this output device"),

        GENERAL_EXCEPTION_SHAPING_TEXT(XRLog.GENERAL, "Exception while shaping text"),
        GENERAL_EXCEPTION_DESHAPING_TEXT(XRLog.GENERAL, "Exception while deshaping text"),

        LAYOUT_ERROR_PARSING_SHAPE_COORDS(XRLog.LAYOUT, "Error while parsing shape coords"),
        LAYOUT_NO_CONTENT_LIMIT_FOUND(XRLog.LAYOUT, "No content limit found"),
        LAYOUT_BOX_HAS_NO_PAGE(XRLog.LAYOUT, "Box has no page"),
        LAYOUT_NO_INLINE_LAYERS(XRLog.LAYOUT, "Boxes with display: inline can not be positioned or transformed, try using inline-block"),

        CASCADE_IS_ABSOLUTE_CSS_UNKNOWN_GIVEN(XRLog.CASCADE, "Asked whether type was absolute, given CSS_UNKNOWN as the type. " +
                "Might be one of those funny values like background-position."),

        MATCH_TRYING_TO_SET_MORE_THAN_ONE_PSEUDO_ELEMENT(XRLog.MATCH, "Trying to set more than one pseudo-element"),

        GENERAL_NO_FOOTNOTES_INSIDE_FOOTNOTES(XRLog.GENERAL, "Footnotes inside footnotes or fixed position area are not supported"),
        GENERAL_IMPORT_FONT_FACE_RULES_HAS_NOT_BEEN_CALLED(XRLog.GENERAL, "importFontFaceRules has not been called for this pdf transcoder"),
        GENERAL_PDF_ACCESSIBILITY_NO_ALT_ATTRIBUTE_PROVIDED_FOR_IMAGE(XRLog.GENERAL, "No alt attribute provided for image/replaced in PDF/UA document."),
        GENERAL_PDF_SPECIFIED_FONTS_DONT_CONTAIN_A_SPACE_CHARACTER(XRLog.GENERAL, "Specified fonts don't contain a space character!"),
        GENERAL_PDF_USING_FAST_MODE(XRLog.GENERAL, "Using fast-mode renderer. Prepare to fly."),
        GENERAL_PDF_ACCESSIBILITY_NO_DOCUMENT_TITLE_PROVIDED(XRLog.GENERAL, "No document title provided. Document will not be PDF/UA compliant."),
        GENERAL_PDF_ACCESSIBILITY_NO_DOCUMENT_DESCRIPTION_PROVIDED(XRLog.GENERAL, "No document description provided. Document will not be PDF/UA compliant."),
        GENERAL_PDF_USING_GET_REQUEST_FOR_FORM(XRLog.GENERAL, "Using GET request method for form. You probably meant to add a method=\"post\" attribute to your form"),
        GENERAL_PDF_ACROBAT_READER_DOES_NOT_SUPPORT_FORMS_WITH_FILE_INPUT(XRLog.GENERAL, "Acrobat Reader does not support forms with file input controls"),

        EXCEPTION_SVG_COULD_NOT_DRAW(XRLog.EXCEPTION, "Couldn't draw SVG."),
        EXCEPTION_SVG_COULD_NOT_READ_FONT(XRLog.EXCEPTION, "Couldn't read font"),
        EXCEPTION_MATHML_COULD_NOT_REGISTER_FONT(XRLog.EXCEPTION, "Could not register font correctly"),
        EXCEPTION_JAVA2D_COULD_NOT_LOAD_FONT(XRLog.EXCEPTION, "Couldn't load font. Please check that it is a valid truetype font."),
        EXCEPTION_PROBLEM_TRYING_TO_READ_INPUT_XHTML_FILE(XRLog.EXCEPTION, "Problem trying to read input XHTML file"),
        EXCEPTION_COULD_NOT_LOAD_FONT_METRICS(XRLog.EXCEPTION, "Couldn't load font metrics."),
        EXCEPTION_UNABLE_TO_PARSE_PAGE_OF_IMG_TAG_WITH_PDF(XRLog.EXCEPTION, "Unable to parse page of img tag with PDF!"),
        EXCEPTION_TRIED_TO_OPEN_A_PASSWORD_PROTECTED_DOCUMENT_AS_SRC_FOR_IMG(XRLog.EXCEPTION, "Tried to open a password protected document as src for an img!"),
        EXCEPTION_COULD_NOT_READ_PDF_AS_SRC_FOR_IMG(XRLog.EXCEPTION, "Could not read pdf passed as src for img element!"),
        EXCEPTION_COULD_NOT_PARSE_DEFAULT_STYLESHEET(XRLog.EXCEPTION, "Could not parse default stylesheet"),
        EXCEPTION_SELECTOR_BAD_SIBLING_AXIS(XRLog.EXCEPTION, "Bad sibling axis");
        

        private final String where;
        private final String messageFormat;

        LogMessageId0Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String getWhere() {
            return where;
        }

        @Override
        public String formatMessage(Object[] args) {
            return getMessageFormat();
        }
    }

    enum LogMessageId1Param implements LogMessageId {
        CSS_PARSE_REMOVING_STYLESHEET_URI_FROM_CACHE_BY_REQUEST(XRLog.CSS_PARSE, "Removing stylesheet '{}' from cache by request."),
        CSS_PARSE_REQUESTED_REMOVING_STYLESHEET_URI_NOT_IN_CACHE(XRLog.CSS_PARSE, "Requested removing stylesheet '{}', but it's not in cache."),

        XML_ENTITIES_SAX_FEATURE_NOT_SUPPORTED(XRLog.XML_ENTITIES, "SAX feature not supported on this XMLReader: {}"),
        XML_ENTITIES_SAX_FEATURE_NOT_RECOGNIZED(XRLog.XML_ENTITIES, "SAX feature not recognized on this XMLReader: {}. Feature may be properly named, but not recognized by this parser."),
        XML_ENTITIES_EXCEPTION_MESSAGE(XRLog.XML_ENTITIES, "{}"),
        XML_ENTITIES_COULD_NOT_OPEN_XML_CATALOG_FROM_URI(XRLog.XML_ENTITIES, "Could not open XML catalog from URI '{}'"),
        XML_ENTITIES_ENTITY_PUBLIC_NO_LOCAL_MAPPING(XRLog.XML_ENTITIES, "Entity public: {}, no local mapping. Returning empty entity to avoid pulling from network."),

        INIT_FONT_COULD_NOT_BE_LOADED(XRLog.INIT, "Font {} could not be loaded"),

        RENDER_OP_MUST_NOT_BE_USED_BY_FAST_RENDERER(XRLog.RENDER, "{} MUST not be used by the fast renderer. Please consider reporting this bug."),
        RENDER_UNKNOWN_PAINT(XRLog.RENDER, "Unknown paint: {}"),
        RENDER_USING_CSS_IMPLEMENTATION_FROM(XRLog.RENDER, "Using CSS implementation from: {}"),
        RENDER_FONT_IS_NULL(XRLog.RENDER, "Font is null for font-description: {}"),

        MATCH_TRYING_TO_APPEND_CONDITIONS_TO_PSEUDO_ELEMENT(XRLog.MATCH, "Trying to append conditions to pseudoElement {}"),
        MATCH_MATCHER_CREATED_WITH_SELECTOR(XRLog.MATCH, "Matcher created with {} selectors"),
        MATCH_MEDIA_IS(XRLog.MATCH, "media = {}"),

        LOAD_COULD_NOT_INSTANTIATE_CUSTOM_XML_READER(XRLog.LOAD, "Could not instantiate custom XMLReader class for XML parsing: {}. " +
                "Please check classpath. Use value 'default' in FS configuration if necessary. Will now try JDK default."),
        LOAD_UNABLE_TO_LOAD_CSS_FROM_URI(XRLog.LOAD, "Unable to load CSS from {}"),
        LOAD_COULD_NOT_LOAD_EMBEDDED_FILE(XRLog.LOAD, "Was not able to load an embedded file for embedding with uri {}"),
        LOAD_PARSE_STYLESHEETS_TIME(XRLog.LOAD, "TIME: parse stylesheets {}ms"),
        LOAD_REQUESTING_STYLESHEET_AT_URI(XRLog.LOAD, "Requesting stylesheet: {}"),
        LOAD_UNRECOGNIZED_IMAGE_FORMAT_FOR_URI(XRLog.LOAD, "Unrecognized image format for: {}"),
        LOAD_URI_RESOLVER_REJECTED_RESOLVING_CSS_IMPORT_AT_URI(XRLog.LOAD, "URI resolver rejected resolving CSS import at ({})"),
        LOAD_URI_RESOLVER_REJECTED_RESOLVING_URI_AT_URI_IN_CSS_STYLESHEET(XRLog.LOAD, "URI resolver rejected resolving URI at ({}) in CSS stylehseet"),
        LOAD_PUTTING_KEY_IN_CACHE(XRLog.LOAD, "Putting key({}) in cache."),
        LOAD_EXCEPTION_MESSAGE(XRLog.LOAD, "{}"),
        LOAD_SAX_FEATURE_NOT_SUPPORTED(XRLog.LOAD, "SAX feature not supported on this XMLReader: {}"),
        LOAD_SAX_FEATURE_NOT_RECOGNIZED(XRLog.LOAD, "SAX feature not recognized on this XMLReader: {}. Feature may be properly named, but not recognized by this parser."),
        LOAD_COULD_NOT_LOAD_PREFERRED_XML(XRLog.LOAD, "Could not load preferred XML {}, using default which may not be secure."),
        LOAD_LOADED_DOCUMENT_TIME(XRLog.LOAD, "Loaded document in ~{}ms"),
        LOAD_SAX_XMLREADER_IN_USE(XRLog.LOAD, "SAX XMLReader in use (parser): {}"),
        LOAD_XMLREADER_CLASS_SPECIFIED_COULD_NOT_BE_FOUND(XRLog.LOAD,"The XMLReader class you specified as a configuration property " +
                "could not be found. Class.forName() failed on " +
                "{}. Please check classpath. Use value 'default' in " +
                "FS configuration if necessary. Will now try JDK default."),
        LOAD_COULD_NOT_RESOLVE_RELATIVE_URI_BECAUSE_NO_BASE_URI_WAS_PROVIDED(XRLog.LOAD, "Couldn't resolve relative URI({}) because no base URI was provided."),
        LOAD_LOAD_IMMEDIATE_URI(XRLog.LOAD, "Load immediate: {}"),

        LAYOUT_FUNCTION_NOT_IMPLEMENTED(XRLog.LAYOUT, "{} function not implemented at this time"),
        LAYOUT_UNSUPPORTED_SHAPE(XRLog.LAYOUT, "Unsupported shape: '{}'"),
        LAYOUT_NO_MAP_NAMED(XRLog.LAYOUT, "No map named: '{}'"),

        GENERAL_MESSAGE(XRLog.GENERAL, "{}"),
        GENERAL_INVALID_INTEGER_PASSED_IN_VIEWBOX_ATTRIBUTE_FOR_SVG(XRLog.GENERAL, "Invalid integer passed in viewBox attribute for SVG: {}"),
        GENERAL_INVALID_INTEGER_PASSED_AS_DIMENSION_FOR_SVG(XRLog.GENERAL, "Invalid integer passed as dimension for SVG: {}"),
        GENERAL_COULD_NOT_FIND_FONT_SPECIFIED_FOR_MATHML_OBJECT_IN_FONT_FACE_RULES(XRLog.GENERAL, "Could not find font ({}) specified for MathML object in font-face rules"),
        GENERAL_PDF_ACCESSIBILITY_NO_TITLE_TEXT_PROVIDED_FOR(XRLog.GENERAL, "PDF/UA - No title text provided for {}."),
        GENERAL_PDF_COULD_NOT_FIND_VALID_TARGET_FOR_BOOKMARK(XRLog.GENERAL, "Could not find valid target for bookmark. Bookmark href = {}"),
        GENERAL_PDF_COULD_NOT_FIND_VALID_TARGET_FOR_LINK(XRLog.GENERAL, "Could not find valid target for link. Link href = {}"),
        GENERAL_PDF_URI_IN_HREF_IS_NOT_A_VALID_URI(XRLog.GENERAL, "'{}' in href is not a valid URI, will be skipped"),
        GENERAL_PDF_FOUND_FORM_CONTROL_WITH_NO_ENCLOSING_FORM(XRLog.GENERAL, "Found form control ({}) with no enclosing form. Ignoring."),
        GENERAL_PDF_A_ELEMENT_DOES_NOT_HAVE_OPTION_CHILDREN(XRLog.GENERAL, "A <{}> element does not have <option> children"),
        GENERAL_FORCED_OUTPUT_TO_AVOID_INFINITE_LOOP(XRLog.GENERAL, "Forced text ({}) output after trying to move to next line unsuccessfully. Probably a bug."),

        GENERAL_FOOTNOTE_INVALID(XRLog.GENERAL, "Element not valid for use as footnote, treating as normal content. Reason: {}. Non-compliant content may be nested in basic <div>."),
        GENERAL_FOOTNOTE_PSEUDO_INVALID(XRLog.GENERAL, "Footnote pseudo element found with invalid style, ignoring. Reason: {}"),
        GENERAL_FOOTNOTE_CAN_NOT_BE_PSEUDO(XRLog.GENERAL, "Pseudo element (::{}) can not have float: footnote set, ignoring."),

        EXCEPTION_FONT_METRICS_NOT_AVAILABLE(XRLog.EXCEPTION, "Font metrics not available for font-description: {}"),
        EXCEPTION_URI_SYNTAX_WHILE_LOADING_EXTERNAL_SVG_RESOURCE(XRLog.EXCEPTION, "URI syntax exception while loading external svg resource: {}"),
        EXCEPTION_SVG_ERROR_HANDLER(XRLog.EXCEPTION, "SVG {}"),
        EXCEPTION_SVG_CREATE_FONT(XRLog.EXCEPTION, "Error while creating a font with family: {}"),
        EXCEPTION_PDF_IN_WRITING_METHOD(XRLog.EXCEPTION, "Exception in PDF writing method: {}"),
        EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI(XRLog.EXCEPTION, "Can't read image file; unexpected problem for URI '{}'"),
        EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI_NOT_FOUND(XRLog.EXCEPTION, "Can't read image file; image at URI '{}' not found"),
        EXCEPTION_COULD_NOT_LOAD_FONT(XRLog.EXCEPTION, "Couldn't load font ({}). Please check that it is a valid truetype font."),
        EXCEPTION_COULD_NOT_CACHE_VALUE_FOR_KEY(XRLog.EXCEPTION, "Could not load cache value for key({})"),
        EXCEPTION_UNHANDLED(XRLog.EXCEPTION, "Unhandled exception. {}"),
        EXCEPTION_MALFORMED_URL(XRLog.EXCEPTION, "Bad URL given: {}"),
        EXCEPTION_ITEM_AT_URI_NOT_FOUND(XRLog.EXCEPTION, "Item at URI {} not found"),
        EXCEPTION_IO_PROBLEM_FOR_URI(XRLog.EXCEPTION, "IO problem for {}"),
        EXCEPTION_CSS_UNABLE_TO_DERIVE_INITIAL_VALUE_FOR_CLASSNAME(XRLog.EXCEPTION, "Unable to derive initial value for {}"),
        EXCEPTION_COULD_NOT_LOAD_FONT_FACE(XRLog.EXCEPTION, "Could not load @font-face font: {}"),
        EXCEPTION_COULD_NOT_LOAD_DEFAULT_CSS(XRLog.EXCEPTION, "Can't load default CSS from {}. This file must be on your CLASSPATH. Please check before continuing."),
        EXCEPTION_DEFAULT_USERAGENT_IS_NOT_ABLE_TO_RESOLVE_BASE_URL_FOR(XRLog.EXCEPTION, "The default NaiveUserAgent doesn't know how to resolve the base URL for {}"),
        EXCEPTION_FAILED_TO_LOAD_BACKGROUND_IMAGE_AT_URI(XRLog.EXCEPTION, "Failed to load background image at uri {}"),
        EXCEPTION_COULD_NOT_LOAD_EMBEDDED_FILE(XRLog.EXCEPTION, "Was not able to create an embedded file for embedding with uri {}");

        private final String where;
        private final String messageFormat;
        private final LogMessageIdFormat logMessageIdFormat;

        LogMessageId1Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
            this.logMessageIdFormat = new LogMessageIdFormat(messageFormat);
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String getWhere() {
            return where;
        }

        @Override
        public String formatMessage(Object[] args) {
            return logMessageIdFormat.formatMessage(args);
        }
    }

    enum LogMessageId2Param implements LogMessageId {
        CSS_PARSE_TOO_MANY_STYLESHEET_IMPORTS(XRLog.CSS_PARSE, "Gave up after {} attempts to load stlyesheet at {} to avoid possible loop"),
        CSS_PARSE_COULDNT_PARSE_STYLESHEET_AT_URI(XRLog.CSS_PARSE, "Couldn't parse stylesheet at URI {}: {}"),
        CSS_PARSE_GENERIC_MESSAGE(XRLog.CSS_PARSE, "({}) {}"),

        XML_FEATURE_NOT_ABLE_TO_SET(XRLog.LOAD, "Could not set XML/SAX feature. This may be a security issue if using untrusted XML. Feature: {} -> {}"),
        XML_ENTITIES_SAX_FEATURE_SET(XRLog.XML_ENTITIES, "SAX Parser feature: {} set to {}"),
        XML_ENTITIES_ENTITY_PUBLIC_NOT_FOUND_OR_LOCAL(XRLog.XML_ENTITIES, "Entity public: {} -> {}"),
        XML_ENTITIES_ENTITY_CANT_FIND_LOCAL_REFERENCE(XRLog.XML_ENTITIES, "Can't find a local reference for Entity for public ID: {}" +
                " and expected to. The local URL should be: {}. Not finding " +
                "this probably means a CLASSPATH configuration problem; this resource " +
                "should be included with the renderer and so not finding it means it is " +
                "not on the CLASSPATH, and should be. Will let parser use the default in " +
                "this case."),

        LAYOUT_CSS_PROPERTY_HAS_UNPROCESSABLE_ASSIGNMENT(XRLog.LAYOUT, "Property {}} has an assignment we don't understand, " +
                "and can't tell if it's an absolute unit or not. Assuming it is not. Exception was: {}"),

        LOAD_LOADING_FONT_FROM_SUPPLIER(XRLog.LOAD, "Loading font({}) from {} supplier now."),
        LOAD_CACHE_HIT_STATUS(XRLog.LOAD, "{} key({}) from cache."),
        LOAD_SAX_FEATURE_SET(XRLog.LOAD, "SAX Parser feature: {} set to {}"),
        LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI(XRLog.LOAD, "URI resolver rejected loading {} at ({})"),
        LOAD_COULD_NOT_READ_URI_AT_URL_MAY_BE_RELATIVE(XRLog.LOAD, "Could not read {} as a URL; may be relative. Testing using parent URL {}"),
        LOAD_WAS_ABLE_TO_READ_FROM_URI_USING_PARENT_URL(XRLog.LOAD, "Was able to read from {} using parent URL {}"),
        LOAD_RESOURCE_ACCESS_REJECTED(XRLog.LOAD, "URI {} with type {} was rejected by resource access controller"),

        GENERAL_FATAL_INFINITE_LOOP_BUG_IN_LINE_BREAKING_ALGO(XRLog.GENERAL, "A fatal infinite loop bug was detected in the line breaking " +
                "algorithm for break-word! Start-substring=[{}], end={}"),
        GENERAL_EXPECTING_BOX_CHILDREN_OF_TYPE_BUT_GOT(XRLog.GENERAL, "Expecting box children to be of type ({}) but got ({})."),
        GENERAL_PDF_FOUND_ELEMENT_WITHOUT_ATTRIBUTE_NAME(XRLog.GENERAL, "found a <{} {}> element without attribute name, the element will not work without this attribute"),
        GENERAL_UNABLE_TO_PARSE_VALUE_AS(XRLog.GENERAL, "Unable to parse value '{}' as {}"),
        GENERAL_FOOTNOTE_AREA_INVALID_STYLE(XRLog.GENERAL, "Invalid value ({}) specified for @footnote area in {} property. Ignoring declaration."),

        EXCEPTION_SVG_EXTERNAL_RESOURCE_NOT_ALLOWED(XRLog.EXCEPTION, "Tried to fetch external resource from SVG. Refusing. Details: {}, {}"),
        EXCEPTION_DEFAULT_USERAGENT_IS_NOT_ABLE_TO_RESOLVE_URL_WITH_BASE_URL(XRLog.EXCEPTION, "The default NaiveUserAgent cannot resolve the URL {} with base URL {}");

        private final String where;
        private final String messageFormat;
        private final LogMessageIdFormat logMessageIdFormat;

        LogMessageId2Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
            this.logMessageIdFormat = new LogMessageIdFormat(messageFormat);
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String formatMessage(Object[] args) {
            return logMessageIdFormat.formatMessage(args);
        }

        @Override
        public String getWhere() {
            return where;
        }
    }

    enum LogMessageId3Param implements LogMessageId {

        GENERAL_PDF_ACCESSIBILITY_INCOMPATIBLE_CHILD(XRLog.GENERAL, "Trying to add incompatible child to parent item: " +
                " child type={}" +
                ", parent type={}" +
                ", expected child type={}" +
                ". Document will not be PDF/UA compliant."),

        EXCEPTION_URI_WITH_BASE_URI_INVALID(XRLog.EXCEPTION, "When trying to load uri({}) with base {} URI({}), one or both were invalid URIs."),
        EXCEPTION_SVG_SCRIPT_NOT_ALLOWED(XRLog.EXCEPTION, "Tried to run script inside SVG. Refusing. Details: {}, {}, {}");


        private final String where;
        private final String messageFormat;
        private final LogMessageIdFormat logMessageIdFormat;

        LogMessageId3Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
            this.logMessageIdFormat = new LogMessageIdFormat(messageFormat);
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String getWhere() {
            return where;
        }

        @Override
        public String formatMessage(Object[] args) {
            return logMessageIdFormat.formatMessage(args);
        }
    }

    enum LogMessageId4Param implements LogMessageId {
        LOAD_IMAGE_LOADER_SCALING_URI_TO(XRLog.LOAD, "{}, scaling {} to {}, {}"),

        CASCADE_UNKNOWN_DATATYPE_FOR_RELATIVE_TO_ABSOLUTE(XRLog.CASCADE, "Asked to convert {} from relative to absolute, don't recognize the datatype '{}' {}({})"),
        CASCADE_CALC_FLOAT_PROPORTIONAL_VALUE_INFO_FONT_SIZE(XRLog.CASCADE, "{}, relative= {} ({}), absolute= {}"),

        EXCEPTION_CONFIGURATION_WRONG_TYPE(XRLog.EXCEPTION, "Property '{}' was requested as a {}, but value of '{}' is not a {}. Check configuration."),;



        private final String where;
        private final String messageFormat;
        private final LogMessageIdFormat logMessageIdFormat;

        LogMessageId4Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
            this.logMessageIdFormat = new LogMessageIdFormat(messageFormat);
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String getWhere() {
            return where;
        }

        @Override
        public String formatMessage(Object[] args) {
            return logMessageIdFormat.formatMessage(args);
        }
    }

    enum LogMessageId5Param implements LogMessageId {

        CASCADE_CALC_FLOAT_PROPORTIONAL_VALUE_INFO(XRLog.CASCADE, "{}, relative= {} ({}), absolute= {} using base={}");

        private final String where;
        private final String messageFormat;
        private final LogMessageIdFormat logMessageIdFormat;

        LogMessageId5Param(String where, String messageFormat) {
            this.where = where;
            this.messageFormat = messageFormat;
            this.logMessageIdFormat = new LogMessageIdFormat(messageFormat);
        }

        @Override
        public Enum<?> getEnum() {
            return this;
        }

        @Override
        public String getMessageFormat() {
            return messageFormat;
        }

        @Override
        public String getWhere() {
            return where;
        }

        @Override
        public String formatMessage(Object[] args) {
            return logMessageIdFormat.formatMessage(args);
        }

    }
}
