package io.github.mainstringargs.alpaca.rest;

/**
 * The Class AssetsUrlBuilder.
 */
public class AssetsRequestBuilder extends AlpacaRequestBuilder {


  /** The Constant ASSETS_ENDPOINT. */
  public final static String ASSETS_ENDPOINT = "assets";



  /**
   * Instantiates a new assets url builder.
   *
   * @param baseUrl the base url
   */
  public AssetsRequestBuilder(String baseUrl) {
    super(baseUrl);
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.github.mainstringargs.alpaca.rest.AlpacaUrlBuilder#endpoint()
   */
  @Override
  public String getEndpoint() {
    return ASSETS_ENDPOINT;
  }

}
