using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace android.Sockets
{
	public static class ClientHelper
    {
		public static bool useHeader = false;
		public static bool serverCheck = false;
		public static void InitialiseCommunication(Socket socket, bool isUseHeader,bool isServerCheck = false)
		{
			useHeader = isUseHeader;
			var b = BitConverter.GetBytes(useHeader ? 1 : 2);
            Array.Reverse(b);
            socket.Send(b);

			serverCheck = isServerCheck;
			if (useHeader)
			{
				b = BitConverter.GetBytes(serverCheck ? 1 : 2);
                Array.Reverse(b);
                socket.Send(b);
			}
		}
		public static void sendFileToServer(byte[] dataBytes, String fileName, Socket server){
			if (!useHeader)
				sendFileToServer1(dataBytes, fileName, server);
			else
				sendFileToServer2(dataBytes, fileName, server);
		}
		private static void sendFileToServer1(byte[] dataBytes, String fileName, Socket server){
            String message = "sending";
            byte[] byteMessage = Encoding.UTF8.GetBytes(message);
            int length = byteMessage.Length;
            byte[] b = BitConverter.GetBytes(length);
			Array.Reverse(b);
			server.Send(b);
            server.Send(byteMessage);

            b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
            int ok = BitConverter.ToInt32(b);
            
			byteMessage = Encoding.UTF8.GetBytes(fileName);
            length = byteMessage.Length;
			b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);
            server.Send(byteMessage);
            
			var byteStream = new MemoryStream(dataBytes);
			length = dataBytes.Length;

			int bufferSize;
			if (length > 524288)
				bufferSize = 524288;
            else
                bufferSize = length;
 
			b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);

			byte[] buffer = new byte[bufferSize];

			b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
            ok = BitConverter.ToInt32(b);
			int count;
			while ((count = byteStream.Read(buffer,0,bufferSize)) > 0)
			{
				server.Send(buffer, count, SocketFlags.None);
            }

			b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
            length = BitConverter.ToInt32(b);
			byteMessage = new byte[length];
			server.Receive(byteMessage);
			String serverMessage = Encoding.UTF8.GetString(byteMessage);
			if (!serverMessage.Equals("recieved"))
				throw new SocketException();

			return ;
		}

		private static void sendFileToServer2(byte[] dataBytes, String fileName, Socket server)
        {
           

            var byteStream = new MemoryStream(dataBytes);
            int length = dataBytes.Length;


			Header header = new Header(SocketMessageHelper.Sending, fileName, length);
			server.Send(header.getByteHeader(), 55, SocketFlags.None);

            int bufferSize;
			if (length > 524288)
				bufferSize = 524288;
            else
                bufferSize = length;

            byte[] buffer = new byte[bufferSize];

            int count;
            while ((count = byteStream.Read(buffer, 0, bufferSize)) > 0)
            {
				server.Send(buffer, count, SocketFlags.None);
            }

			if (serverCheck)
            {
				byte[] b = new byte[1];
				server.Receive(b, 1, SocketFlags.None);
                if (b[0] != 1)
                    throw new IOException("Server not ok!");
            }

            return;
        }

		public class FileTestData
        {
            public String name;
            public byte[] data;

            public FileTestData() { }

            public FileTestData(String name, byte[] data)
            {
                this.name = name;
                this.data = data;
            }
        }

		public static FileTestData reachFileFromServer(Socket server, String fileName){
			if (!useHeader)
				return reachFileFromServer1(server, fileName);
			else
				return reachFileFromServer2(server, fileName);
		}

		private static FileTestData reachFileFromServer1(Socket server, String fileName){  
			
            String message = "requesting";
            byte[] byteMessage = Encoding.UTF8.GetBytes(message);
            int length = byteMessage.Length;
			byte[] b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);
            server.Send(byteMessage);

            b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
			int ok = BitConverter.ToInt32(b);

            byteMessage = Encoding.UTF8.GetBytes(fileName);
            length = byteMessage.Length;
			b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);
            server.Send(byteMessage);

			b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
            length = BitConverter.ToInt32(b);
            byteMessage = new byte[length];
            server.Receive(byteMessage);
            String serverMessage = Encoding.UTF8.GetString(byteMessage);

            b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
            length = BitConverter.ToInt32(b);

            int bufferSize;
			if (length > 524288)
				bufferSize = 524288;
            else
                bufferSize = length;

            b = new byte[4];
			server.Receive(b);
            Array.Reverse(b);
			ok = BitConverter.ToInt32(b);
			Thread.Sleep(1);
			var remoteEndPoint = server.RemoteEndPoint as IPEndPoint;
            remoteEndPoint.Port += 1;
            byte[] buffer = new byte[bufferSize];
            byte[] bytes = new byte[length];
			MemoryStream dataBuffer = new MemoryStream();
			var count = length;
			int dataRead;
			while ((dataRead = server.Receive(buffer, bufferSize, SocketFlags.None)) != -1 )
            {
				count -= dataRead;
				dataBuffer.Write(buffer, 0, dataRead);
                if (count <= 0)
                    break;
				if (count <= bufferSize)
					bufferSize = count;
            }

			Array.Copy(dataBuffer.ToArray(), bytes, length);
			dataBuffer.Close();

			message = "recieved";
			byteMessage = Encoding.UTF8.GetBytes(message);
            length = byteMessage.Length;
			b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);
            server.Send(byteMessage);

			return new FileTestData(fileName, bytes);
		}

		public static FileTestData reachFileFromServer2(Socket server, String fileName)
        {

            Header header = new Header(SocketMessageHelper.Requesting, fileName, 0);
			server.Send(header.getByteHeader(), 55, SocketFlags.None);

            byte []b = new byte[4];
			server.Receive(b, 4, SocketFlags.None);
            Array.Reverse(b);
            int length = BitConverter.ToInt32(b);

            int bufferSize;
			if (length > 524288)
				bufferSize = 524288;
            else
                bufferSize = length;

            byte[] buffer = new byte[bufferSize];
            byte[] bytes = new byte[length];
            MemoryStream dataBuffer = new MemoryStream();
			var count = length;
            int dataRead;
            while ((dataRead = server.Receive(buffer, bufferSize, SocketFlags.None)) != -1)
            {
                count -= dataRead;
                dataBuffer.Write(buffer, 0, dataRead);
                if (count <= 0)
                    break;
                if (count <= bufferSize)
                    bufferSize = count;
            }

            Array.Copy(dataBuffer.ToArray(), bytes, length);
            dataBuffer.Close();

            return new FileTestData(fileName, bytes);
        }
		public static void endConnection(Socket server){         
            if (!useHeader)
                endConnection1(server);
            else
                endConnection2(server);
		}
		private static void endConnection1(Socket server){
			String message = "end";
            byte[] byteMessage = Encoding.UTF8.GetBytes(message);
            int length = byteMessage.Length;
            byte[] b = BitConverter.GetBytes(length);
            Array.Reverse(b);
            server.Send(b);
            server.Send(byteMessage);

            server.Disconnect(true);
			server.Close();
		}

        private static void endConnection2(Socket server)
        {

            Header header = new Header(SocketMessageHelper.End, "", 0);
			server.Send(header.getByteHeader(), 55, SocketFlags.None);

            server.Disconnect(true);
            server.Close();
        }

		public static void saveFileToMemory(byte[] dataBytes, String fileName){
			var startTime = DateTime.Now;
			var documents = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
			var filename = Path.Combine(documents, fileName);
			File.WriteAllBytes(filename, dataBytes);
			return;
		}

		public static FileTestData reachFromMemory(String fileName){
			var startTime = DateTime.Now;
            var documents = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
            var filename = Path.Combine(documents, fileName);
			byte[] dataBytes = File.ReadAllBytes(filename);
			return new FileTestData(fileName, dataBytes);
		}
        
		public static byte[] generateFile(int fileSize){
            byte[] dataBytes = new byte[fileSize];
            new Random().NextBytes(dataBytes);
            return dataBytes;
		}

		public static void saveResult(int fileSize, double transferTime, String testName, String transferMedia, String action)
        {
            String results = "\n"
				    + DateTime.Now.ToString("R")
                    + ";" + action
				    + ";" + fileSize.ToString()
                    + ";" + transferTime.ToString()
                    + ";" + transferMedia
                    + ";" + testName + ";;;";

			String resultsFileName = "Results.csv";
            var documents = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
			var filename = Path.Combine(documents, resultsFileName);
			File.AppendAllText(filename, results);
        }
      
    }
}
