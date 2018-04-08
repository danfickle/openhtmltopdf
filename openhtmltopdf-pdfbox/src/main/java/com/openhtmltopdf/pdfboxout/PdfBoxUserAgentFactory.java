package com.openhtmltopdf.pdfboxout;

/**
 *  PdfBoxUserAgentFactory. Singleton.
 */
public class PdfBoxUserAgentFactory {

    private static class LazyFactoryHolder {
        private static final PdfBoxUserAgentFactory INSTANCE = new PdfBoxUserAgentFactory();
    }

    protected static PdfBoxUserAgentFactory instance() {
        return LazyFactoryHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final UserAgentFactory INSTANCE = new UserAgentFactory() {
            public PdfBoxUserAgent getPdfBoxUserAgent(PdfBoxOutputDevice outputDevice) {
                return new PdfBoxUserAgent(outputDevice);
            }
        };
    }

    /*
     * In all versions of Java, the idiom enables a safe, highly concurrent lazy initialization with good performance.
     */
    private static UserAgentFactory factoryDef() {
        return LazyHolder.INSTANCE;
    }

    private ThreadLocal<UserAgentFactory> threadLocal;
    private UserAgentFactory global;


    {
        threadLocal = new ThreadLocal<UserAgentFactory>();
    }

    /**
     * Register [global] own / custom PdfBoxUserAgentFactory
     * @param factory - implementation of UserAgentFactory interface
     */
    public void registerGlobalPdfBoxUserAgentFactory(UserAgentFactory factory) {
        this.global = factory;
    }

    /**
     * UnRegister [global] own / custom PdfBoxUserAgentFactory
     */
    public void unregisterGlobalPdfBoxUserAgentFactory() {
        this.global = null;
    }

    /**
     * Register [threadLocal] own / custom PdfBoxUserAgentFactory
     * @param factory - implementation of UserAgentFactory interface
     */
    public  void registerPdfBoxUserAgentFactory(UserAgentFactory factory) {
        threadLocal.set(factory);
    }

    /**
     * UnRegister [threadLocal] own / custom PdfBoxUserAgentFactory
     */
    public  void unregisterPdfBoxUserAgentFactory() {
        threadLocal.remove();
    }

    /**
     *
     * @param outputDevice - instance of PdfBoxOutputDevice, mandatory field
     * @return instanceof PdfBoxUserAgent
     */
    public static PdfBoxUserAgent getPdfBoxUserAgent(PdfBoxOutputDevice outputDevice) {
        return instance().getPdfBoxUserAgent0(outputDevice);
    }


    private PdfBoxUserAgent getPdfBoxUserAgent0(PdfBoxOutputDevice outputDevice) {
        UserAgentFactory thLocal = threadLocal.get();
        return (thLocal != null ? thLocal : global != null ? global : factoryDef()).getPdfBoxUserAgent(outputDevice);
    }


    public interface UserAgentFactory {
        /**
         * Factory to use custom implementation of PdfBoxUserAgent classes
         * @param outputDevice
         * @return
         */
        public PdfBoxUserAgent getPdfBoxUserAgent(PdfBoxOutputDevice outputDevice);
    }


}
