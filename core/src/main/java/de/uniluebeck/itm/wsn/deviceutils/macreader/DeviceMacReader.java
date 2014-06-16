/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.wsn.deviceutils.macreader;

import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import javax.annotation.Nullable;
import java.util.Map;

public interface DeviceMacReader {

	/**
	 * <p>
	 * Reads the MAC address of the device at port {@code}.
	 * </p>
	 * <p>
	 * If the device does not support reading MAC addresses using hardware functionality this method will try to
	 * use the given {@code reference} as the USB-to-serial converters ID. This ID will then be looked up in the
	 * {@link DeviceMacReferenceMap} instance. If present the MAC address found in this map will be returned.
	 * </p>
	 *
	 * @param port
	 * 		the device port
	 * @param deviceType
	 * 		the type of the device (e.g., "isense", "telosb")
	 * @param configuration
	 * 		arbitrary configuration parameters to be passed {@link de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory}
	 * @param reference
	 * 		the USB-to-serial converter ID of this device
	 *
	 * @return the MAC address of the attached or {@code null} if it could not be determined
	 */
	MacAddress readMac(final String port,
					   final String deviceType,
					   @Nullable Map<String, String> configuration,
					   @Nullable final String reference);

}
