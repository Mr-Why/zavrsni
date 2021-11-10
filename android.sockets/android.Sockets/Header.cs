using System;
using System.Text;

namespace android.Sockets
{
    public class Header
    {
		public byte message;
        public String name;
        public int length;

        public Header(byte message, String name, int length)
        {
            this.message = message;
            this.name = name;
            this.length = length;
        }         
    

        public byte[] getByteHeader()
        {
            byte[] byteHeader = new byte[55];
            byteHeader[0] = message;
    		byte[] byteName = Encoding.UTF8.GetBytes(name);

			Buffer.BlockCopy(byteName, 0, byteHeader, 1, byteName.Length);
			Buffer.BlockCopy(BitConverter.GetBytes(length), 0, byteHeader, 51, 4);
            return byteHeader;
        }
    }
}
