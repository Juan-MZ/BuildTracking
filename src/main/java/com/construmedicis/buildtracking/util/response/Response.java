package com.construmedicis.buildtracking.util.response;

/**
 * Clase que define un DTO generico para las respuestas de las peticiones a los
 * servicios Rest. Este encapsula el resultado del consumo de las Apis y le
 * adiciona atributos de control
 * 
 * @author
 *
 * @param <T> Objeto de respuesta para las transacciones realizadas en cada Api
 * 
 * @version 1.0
 * 
 */
public class Response<T> {

	/** Indica el estado de la transaccion */
	private int status;

	/** Mensaje informativo para el usuario */
	private String userMessage;

	/** Url para consultar mas informacion acerca del error */
	private String moreInfo;

	/** Objeto con la respuesta de la transaccion */
	private T data;

	/**
	 * @return the state
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @param status the state to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @return the userMessage
	 */
	public String getUserMessage() {
		return userMessage;
	}

	/**
	 * @param userMessage the userMessage to set
	 */
	public void setUserMessage(String userMessage) {
		this.userMessage = userMessage;
	}

	/**
	 * @return the moreInfo
	 */
	public String getMoreInfo() {
		return moreInfo;
	}

	/**
	 * @param moreInfo the moreInfo to set
	 */
	public void setMoreInfo(String moreInfo) {
		this.moreInfo = moreInfo;
	}

	/**
	 * @return the data
	 */
	public T getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Response [state=" + status + ", userMessage=" + userMessage + ", moreInfo=" + moreInfo + ", data="
				+ data + "]";
	}
}
