using Android.App;
using Android.Widget;
using Android.OS;
using System;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace android.Sockets
{
    [Activity(Label = "android.Sockets", MainLauncher = true, Icon = "@mipmap/icon")]
    public class MainActivity : Activity
    {

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            SetContentView(Resource.Layout.Main);
			Button test1Button = FindViewById<Button>(Resource.Id.button1);
			test1Button.Click += OnTestStart;

            Button test2Button = FindViewById<Button>(Resource.Id.button2);
			test2Button.Click += OnTestStart;
            
            Button test3Button = FindViewById<Button>(Resource.Id.button3);
            test3Button.Click += OnTestStart;


        }
		void OnTestStart (object s,EventArgs  e){
			Task.Run(async () => await TestStart(((Button)s).Id));
		}

		private async Task TestStart(int senderId)
		{         
			bool useHeader = senderId != Resource.Id.button1;
			bool serverCheck = senderId == Resource.Id.button3;
			var portView = FindViewById<EditText>(Resource.Id.portNumberText);
			var ipView = FindViewById<EditText>(Resource.Id.ipText);
            
			var ip = ipView.Text;
			int port;
			Int32.TryParse(portView.Text, out port);

			try
			{
				IPEndPoint serverAddres = new IPEndPoint(IPAddress.Parse(ip), port);

				Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
				socket.NoDelay = true;
				socket.Connect(serverAddres);
				WriteToConsole("Connected to" + serverAddres);
				ClientHelper.InitialiseCommunication(socket, useHeader, serverCheck);
				var fileSize = 1024;
				var fileName = "testFile";
				var testName = "ios_test";
				for (int step = 1; step <= 32768; step *= 2)
				{
					fileSize = 1024 * step;
					var file = ClientHelper.generateFile(fileSize);
					WriteToConsole("Initiating server send tests with" + fileSize + "kB");
					var startTime = DateTime.Now;
					for (int i = 0; i < 50; i++)
					{
						ClientHelper.sendFileToServer(file, fileName, socket);
					}
					var averageTime = (DateTime.Now - startTime).Milliseconds / 50.0;
					ClientHelper.saveResult(fileSize, averageTime, testName, "net", "server write");


                    WriteToConsole("Initiating server recieve tests with" + fileSize + "kB");
					startTime = DateTime.Now;
					for (int i = 0; i < 50; i++)
					{
						var data = ClientHelper.reachFileFromServer(socket, fileName);
					}
					averageTime = (DateTime.Now - startTime).Milliseconds / 50.0;
					ClientHelper.saveResult(fileSize, averageTime, testName, "net", "server read");


                    WriteToConsole("Initiating memory write tests with" + fileSize + "kB");
					startTime = DateTime.Now;
					for (int i = 0; i < 50; i++)
					{
						ClientHelper.saveFileToMemory(file, fileName);
					}
					averageTime = (DateTime.Now - startTime).Milliseconds / 50.0;
					ClientHelper.saveResult(fileSize, averageTime, testName, "android memory", "memory write");


                    WriteToConsole("Initiating memory read tests with" + fileSize + "kB");
					startTime = DateTime.Now;
					for (int i = 0; i < 50; i++)
					{
						var data = ClientHelper.reachFromMemory(fileName);
					}
					averageTime = (DateTime.Now - startTime).Milliseconds / 50.0;
					ClientHelper.saveResult(fileSize, averageTime, testName, "android memory", "memory read");
				}
				ClientHelper.endConnection(socket);
			}
			catch (Exception ex)
			{
				WriteToConsole(ex.StackTrace);
			}


            await System.Threading.Tasks.Task.Delay(0);
        }

		public void WriteToConsole(string message){

            var console = FindViewById<TextView>(Resource.Id.textViewConsole);
            var scroll = FindViewById<ScrollView>(Resource.Id.scrollView);

            RunOnUiThread(() => {
				console.Text += "\n>" + message;
                scroll.FullScroll(Android.Views.FocusSearchDirection.Down);

            });
		}

    }
}

