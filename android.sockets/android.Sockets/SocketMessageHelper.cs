using System;
namespace android.Sockets
{
    public static class SocketMessageHelper
	{
		public static byte Sending => 0;
		public static byte Requesting => 1;
		public static byte Viewing => 2;
		public static byte End => 3;

	}
}
